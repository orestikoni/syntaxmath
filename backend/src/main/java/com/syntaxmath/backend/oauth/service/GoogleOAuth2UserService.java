package com.syntaxmath.backend.oauth.service;

import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.entity.OAuthIdentity;
import com.syntaxmath.backend.auth.interfaces.OAuthIdentityRepository;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class GoogleOAuth2UserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final String PROVIDER = "google";

    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository userRepository;
    private final OAuthIdentityRepository identityRepository;

    public GoogleOAuth2UserService(UserRepository userRepository,
                                   OAuthIdentityRepository identityRepository) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
    }

    @Override
    public OidcUser loadUser(@NonNull OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        Boolean emailVerified = oidcUser.getClaim("email_verified");
        if (emailVerified == null || !emailVerified) {
            throw new OAuth2AuthenticationException("Google account email is not verified");
        }

        String rawEmail = oidcUser.getEmail();
        if (rawEmail == null) {
            throw new OAuth2AuthenticationException("Google account did not return an email address");
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        String subject = oidcUser.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new OAuth2AuthenticationException("Google account did not return a stable subject");
        }

        var linkedIdentity = identityRepository.findByProviderAndProviderSubject(PROVIDER, subject);
        User user = linkedIdentity
                .flatMap(identity -> userRepository.findById(identity.getUserId()))
                .orElseGet(() -> findOrCreateUser(email, oidcUser));
        if (!user.isEmailVerified()) {
            user.markEmailVerified();
            user = userRepository.saveAndFlush(user);
        }
        if (linkedIdentity.isEmpty()) {
            if (identityRepository.existsByProviderAndUserId(PROVIDER, user.getId())) {
                throw new OAuth2AuthenticationException(
                        "A different Google identity is already linked to this account"
                );
            }
            try {
                identityRepository.saveAndFlush(new OAuthIdentity(user.getId(), PROVIDER, subject));
            } catch (DataIntegrityViolationException exception) {
                User linkedUser = identityRepository.findByProviderAndProviderSubject(PROVIDER, subject)
                        .flatMap(identity -> userRepository.findById(identity.getUserId()))
                        .orElseThrow(() -> new OAuth2AuthenticationException(
                                "Google identity is already linked to another account"
                        ));
                user = linkedUser;
            }
        }

        return new SyntaxMathOidcUser(oidcUser, user);
    }

    private User findOrCreateUser(String email, OidcUser oidcUser) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            try {
                return userRepository.saveAndFlush(new User(
                        email,
                        null,
                        oidcUser.getGivenName(),
                        oidcUser.getFamilyName()
                ));
            } catch (DataIntegrityViolationException exception) {
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new OAuth2AuthenticationException(
                                "Unable to create or locate Google account"
                        ));
            }
        });
    }
}

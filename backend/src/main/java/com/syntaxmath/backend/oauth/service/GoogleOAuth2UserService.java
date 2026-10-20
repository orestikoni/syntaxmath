package com.syntaxmath.backend.oauth.service;

import com.syntaxmath.backend.auth.entity.User;
import com.syntaxmath.backend.auth.interfaces.UserRepository;
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

    private final OidcUserService delegate = new OidcUserService();
    private final UserRepository userRepository;

    public GoogleOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
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

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(new User(
                        email,
                        null,
                        oidcUser.getGivenName(),
                        oidcUser.getFamilyName()
                )));
        if (!user.isEmailVerified()) {
            user.markEmailVerified();
            user = userRepository.save(user);
        }

        return new SyntaxMathOidcUser(oidcUser, user);
    }
}

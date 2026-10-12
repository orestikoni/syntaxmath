package com.syntaxmath.backend.oauth.service;

import com.syntaxmath.backend.auth.entity.User;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class SyntaxMathOidcUser extends DefaultOidcUser {

    private final User appUser;

    public SyntaxMathOidcUser(OidcUser oidcUser, User appUser) {
        super(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo());
        this.appUser = appUser;
    }

    public User getAppUser() {
        return appUser;
    }
}

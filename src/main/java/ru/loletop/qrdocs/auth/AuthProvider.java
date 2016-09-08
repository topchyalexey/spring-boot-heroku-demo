package ru.loletop.qrdocs.auth;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class AuthProvider implements AuthenticationProvider {

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        
        AuthToken auth = (AuthToken) authentication;        
        Object user = auth.getPrincipal();
        
        if(user == null){
            throw new RuntimeException("Could not find user with ID: " + auth.getName());
        }
        
        return auth;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AuthToken.class.isAssignableFrom(authentication);
    }

}

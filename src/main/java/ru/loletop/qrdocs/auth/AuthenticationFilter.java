package ru.loletop.qrdocs.auth;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String xAuth = request.getHeader("X-Auth");
        
        // validate the value in xAuth
        if(isValid(xAuth) == false){
            throw new SecurityException();
        }                            
        // Create our Authentication and let Spring know about it
        Authentication auth = AuthToken.instance();
        SecurityContextHolder.getContext().setAuthentication(auth);            
        
        filterChain.doFilter(request, response);
    }

	private boolean isValid(String xAuth) {
		// TODO Auto-generated method stub
		return false;
	}

}


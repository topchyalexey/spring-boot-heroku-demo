package ru.loletop.qrdocs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import ru.loletop.qrdocs.auth.AuthProvider;
import ru.loletop.qrdocs.auth.AuthenticationFilter;

/**
 * Created by topchy on 06.09.16.
 */
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
        	.withUser("user").password("password").roles("USER").and()
        	.withUser("admin").password("password").roles("USER", "ADMIN");

    }

    @Autowired
    private AuthProvider authenticationProvider;

    @Configuration
    @Order(1)
    public static class ApiWebSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {
        protected void configure(HttpSecurity http) throws Exception {
            
                http
                .authorizeRequests()
                    .antMatchers("/user")
                        .addFilterBefore(new AuthenticationFilter(), BasicAuthenticationFilter.class)                
                ;
                                
        }
            
            @Override
            public void configure(AuthenticationManagerBuilder auth) throws Exception {        
                auth.authenticationProvider(demoAuthenticationProvider);        
 
        }
    }

    @Configuration
    public static class FormLoginWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        @Override
        public void configure(WebSecurity web) throws Exception {
            web
            	.ignoring()
                    .antMatchers("/resources/**");
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .authorizeRequests()
                    .antMatchers("/signup","/about").permitAll()
                    .anyRequest().hasRole("USER")
                    .and()
                .formLogin()
                    .loginPage("/login")
                    .permitAll();
        }
    }    

}
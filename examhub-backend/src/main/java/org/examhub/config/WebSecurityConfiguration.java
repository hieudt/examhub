package org.examhub.config;

import org.examhub.domain.UserAccount;
import org.examhub.repository.UserAccountRepository;
import org.examhub.web.filter.CsrfCookieGeneratorFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfFilter;

import javax.servlet.http.HttpServletResponse;

/**
 * @author Hieu Do
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            UserAccount userAccount = userAccountRepository.findByUsernameIgnoreCase(username);
            if (userAccount == null) {
                throw new UsernameNotFoundException("Username " + username + " was not found in database");
            }
            return userAccount;
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService()).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .csrf()
                .and()
            .addFilterAfter(new CsrfCookieGeneratorFilter(), CsrfFilter.class)
                .exceptionHandling()
                .authenticationEntryPoint((request, response, exception) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                })
                .and()
            .formLogin()
                .loginProcessingUrl("/api/v1/authenticate")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                })
                .failureHandler((request, response, exception) -> {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, exception.getMessage());
                })
                .usernameParameter(Constants.FORM_LOGIN_PARAM_USERNAME)
                .passwordParameter(Constants.FORM_LOGIN_PARAM_PASSWORD)
                .permitAll()
                .and()
            .authorizeRequests()
                .antMatchers("/api/v1/register").permitAll()
                .antMatchers("/api/**").authenticated();
        // @formatter:on
    }
}

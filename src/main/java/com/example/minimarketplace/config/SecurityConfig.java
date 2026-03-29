package com.example.minimarketplace.config;

import jakarta.servlet.RequestDispatcher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

/**
 * Spring Security configuration.
 *
 * URL access rules:
 *   /                        → everyone (guest sees limited view)
 *   /search                  → everyone
 *   /auth/**                 → everyone (login, register)
 *   /css/**, /js/**, /images → everyone (static resources)
 *   /admin/**                → ROLE_ADMIN only
 *   /seller/**               → ROLE_SELLER only
 *   /buyer/**                → any authenticated user
 *   anything else            → must be authenticated
 *
 * Spring Boot auto-wires DaoAuthenticationProvider when it finds
 * a UserDetailsService bean + a PasswordEncoder bean in context.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    public static final String AUTH_ERROR_MESSAGE_SESSION_KEY = "authErrorMessage";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .exceptionHandling(ex -> ex
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    if (accessDeniedException instanceof CsrfException && isAuthFormRequest(request.getRequestURI())) {
                        request.getSession(true).setAttribute(
                            AUTH_ERROR_MESSAGE_SESSION_KEY,
                            resolveAuthFormErrorMessage(request.getRequestURI())
                        );
                        response.sendRedirect(request.getRequestURI());
                        return;
                    }

                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, HttpStatus.FORBIDDEN.value());
                    request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, accessDeniedException);
                    request.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, request.getRequestURI());
                    request.setAttribute("errorTitle", resolveAccessDeniedTitle(accessDeniedException));
                    request.setAttribute("errorMessage", resolveAccessDeniedMessage(accessDeniedException));
                    request.setAttribute("errorType", accessDeniedException.getClass().getSimpleName());
                    request.getRequestDispatcher("/access-denied").forward(request, response);
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/", "/search").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/access-denied").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                // Role-gated
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/seller/**").hasRole("SELLER")
                .requestMatchers("/buyer/**").authenticated()
                // Everything else needs login
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/auth/login")
                .loginProcessingUrl("/auth/login")  // Spring Security handles the POST
                .usernameParameter("username")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureUrl("/auth/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/auth/logout")          // POST
                .logoutSuccessUrl("/?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public HttpFirewall httpFirewall() {
        // Support URL-based sessions like /auth/login;jsessionid=...
        // for browsers/environments that don't send cookies.
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer(HttpFirewall httpFirewall) {
        return web -> web.httpFirewall(httpFirewall);
    }

    private String resolveAccessDeniedTitle(AccessDeniedException ex) {
        if (ex instanceof CsrfException) {
            return "Invalid form token";
        }
        return "Access denied";
    }

    private String resolveAccessDeniedMessage(AccessDeniedException ex) {
        if (ex instanceof CsrfException) {
            return "Your session token is missing or expired. Refresh the page and submit the form again.";
        }
        return "You don't have permission to access this page.";
    }

    private String resolveAuthFormErrorMessage(String requestUri) {
        if ("/auth/login".equals(requestUri) || "/auth/login/".equals(requestUri)) {
            return "Your session expired. Please try again.";
        }
        return "Your session expired. Please submit the form again.";
    }

    private boolean isAuthFormRequest(String requestUri) {
        return "/auth/login".equals(requestUri)
            || "/auth/login/".equals(requestUri)
            || "/auth/register".equals(requestUri)
            || "/auth/register/".equals(requestUri);
    }
}

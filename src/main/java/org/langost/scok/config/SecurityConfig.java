package org.langost.scok.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.langost.scok.config.tokens.JwtService;
import org.langost.scok.entity.User;
import org.langost.scok.entity.UserPrincipal;
import org.langost.scok.repository.UserRepository;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.langost.scok.service.CognitoUserService;
import org.langost.scok.controller.AuthController;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CognitoUserService cognitoUserService;
    private final AuthController authController;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                 http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieCsrfTokenRepository()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context -> context.securityContextRepository(new NullSecurityContextRepository()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/me", "/ws", "/ws/**").authenticated()
                        .requestMatchers("/api/auth/**",
                                        "/error",
                                        "/",
                                        "/*.html",
                                        "/*.css",
                                        "/*.js",
                                        "/oauth2/**",
                                        "/login/oauth2/**"

                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth.loginPage("/login.html")
                        .authorizedClientRepository(authController.clients())
                        .userInfoEndpoint(info -> info.oidcUserService(request -> {
                            var identity = new OidcUserService().loadUser(request);
                            cognitoUserService.resolveOrCreate(identity);
                            return identity;
                        }))
                        .successHandler(authController::completeLogin)
                        .failureUrl("/login.html?error"))
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(this::accessCookie)
                        .jwt(jwt -> jwt
                        .decoder(jwtService::decodeAccessToken)
                        .jwtAuthenticationConverter(this::localUserAuthentication)));

        return http.build();
    }

    private String accessCookie(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path.startsWith("/api/auth/") && !path.equals("/api/auth/me")) return null;
        if (!path.startsWith("/api/") && !path.equals("/ws") && !path.startsWith("/ws/")) return null;
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (AuthController.ACCESS_COOKIE.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private AbstractAuthenticationToken localUserAuthentication(Jwt jwt) {
        User user = userRepository.findByCognitoSub(jwt.getSubject())
                .orElseThrow(() -> new UsernameNotFoundException("Local profile not found"));
        UserPrincipal principal = new UserPrincipal(user);
        return new UsernamePasswordAuthenticationToken(principal, jwt, principal.getAuthorities());
    }

}

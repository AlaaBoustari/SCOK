package org.langost.scok.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.langost.scok.dto.request.LoginRequest;
import org.langost.scok.dto.request.SignUpRequest;
import org.langost.scok.dto.response.AuthResponse;
import org.langost.scok.entity.UserPrincipal;
import org.langost.scok.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    public static final String REFRESH_COOKIE = "scok_refresh";
    public static final String ACCESS_COOKIE = "scok_access";

    private final AuthService authService;
    private final OAuth2AuthorizedClientRepository clients = new HttpSessionOAuth2AuthorizedClientRepository();

    @Value("${app.cognito.refresh-cookie-max-age-seconds:432000}")
    private long refreshCookieMaxAge;

    @GetMapping("/csrf")
    public CsrfToken csrf(CsrfToken token) {
        return token;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Boolean>> signup(@Valid @RequestBody SignUpRequest signup) {
        boolean confirmed = authService.signup(signup);
        return ResponseEntity.ok(Map.of("confirmed", confirmed));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest login,
            HttpServletRequest request, HttpServletResponse response) {

        AuthService.Tokens tokens = authService.login(login);
        AuthResponse body = authService.profile(tokens.accessToken());
        cookie(response, ACCESS_COOKIE, tokens.accessToken(),
                authService.accessTokenMaxAge(tokens.accessToken()), request.isSecure());
        cookie(response, REFRESH_COOKIE, tokens.refreshToken(), refreshCookieMaxAge, request.isSecure());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@Valid @RequestBody ConfirmationRequest confirmation) {
        authService.confirmSignup(confirmation.email(), confirmation.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/resend-confirmation")
    public ResponseEntity<Void> resend(@Valid @RequestBody EmailRequest email) {
        authService.resendConfirmation(email.email());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new AuthResponse(principal.getUser().getId(), principal.getUser().getUsername()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest request, HttpServletResponse response) {
        AuthService.Tokens tokens;
        try {
            tokens = authService.refresh(refreshToken);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode().value() == 401) {
                cookie(response, REFRESH_COOKIE, "", 0, request.isSecure());
                cookie(response, ACCESS_COOKIE, "", 0, request.isSecure());
            }
            throw ex;
        }
        AuthResponse body = authService.profile(tokens.accessToken());
        cookie(response, ACCESS_COOKIE, tokens.accessToken(),
                authService.accessTokenMaxAge(tokens.accessToken()), request.isSecure());
        if (tokens.refreshToken() != null) {
            cookie(response, REFRESH_COOKIE, tokens.refreshToken(), refreshCookieMaxAge, request.isSecure());
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            authService.logout(refreshToken);
        } finally {
            cookie(response, REFRESH_COOKIE, "", 0, request.isSecure());
            cookie(response, ACCESS_COOKIE, "", 0, request.isSecure());
        }
        return ResponseEntity.noContent().build();
    }

    public void completeLogin(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauth = (OAuth2AuthenticationToken) authentication;
        OAuth2AuthorizedClient client = clients.loadAuthorizedClient(
                oauth.getAuthorizedClientRegistrationId(), oauth, request);
        if (client == null || client.getRefreshToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cognito did not supply login tokens");
        }
        cookie(response, ACCESS_COOKIE, client.getAccessToken().getTokenValue(),
                authService.accessTokenMaxAge(client.getAccessToken().getTokenValue()), request.isSecure());
        cookie(response, REFRESH_COOKIE, client.getRefreshToken().getTokenValue(),
                refreshCookieMaxAge, request.isSecure());

        clients.removeAuthorizedClient(oauth.getAuthorizedClientRegistrationId(), oauth, request, response);
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        response.addHeader(HttpHeaders.SET_COOKIE,
                ResponseCookie.from("JSESSIONID", "").path("/").maxAge(0).build().toString());
        SecurityContextHolder.clearContext();
        response.sendRedirect("/rooms.html");
    }

    public OAuth2AuthorizedClientRepository clients() {
        return clients;
    }

    private void cookie(HttpServletResponse response, String name, String value, long maxAge, boolean secure) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true).secure(secure).sameSite("Lax")
                .path(ACCESS_COOKIE.equals(name) ? "/" : "/api/auth").maxAge(maxAge).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public record ConfirmationRequest(@Email @NotBlank String email, @NotBlank String code) {}
    public record EmailRequest(@Email @NotBlank String email) {}
}

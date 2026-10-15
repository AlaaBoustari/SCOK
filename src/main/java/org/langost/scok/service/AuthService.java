package org.langost.scok.service;

import lombok.RequiredArgsConstructor;
import org.langost.scok.config.tokens.JwtService;
import org.langost.scok.dto.request.LoginRequest;
import org.langost.scok.dto.response.AuthResponse;
import org.langost.scok.entity.User;
import org.langost.scok.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import  software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Map;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final CognitoIdentityProviderClient cognito;
    private final ClientRegistrationRepository registrations;
    private final JwtService jwtService;
    private final UserRepository users;
    private final CognitoUserService cognitoUserService;

    public record Tokens(String accessToken, String refreshToken) {}

    public boolean signup(org.langost.scok.dto.request.SignUpRequest signup) {
        SignUpResponse result = cognito.signUp(SignUpRequest.builder()
                .clientId(registrations.findByRegistrationId("cognito").getClientId())
                .username(signup.email())
                .password(signup.password())
                .secretHash(secretHash(signup.email()))
                .userAttributes(
                        AttributeType.builder().name("email").value(signup.email()).build(),
                        AttributeType.builder().name("preferred_username").value(signup.username()).build())
                .build());
        return Boolean.TRUE.equals(result.userConfirmed());
    }

    public void confirmSignup(String email, String code) {
        cognito.confirmSignUp(ConfirmSignUpRequest.builder()
                .clientId(registrations.findByRegistrationId("cognito").getClientId())
                .username(email)
                .confirmationCode(code)
                .secretHash(secretHash(email))
                .build());
    }

    public void resendConfirmation(String email) {
        cognito.resendConfirmationCode(ResendConfirmationCodeRequest.builder()
                .clientId(registrations.findByRegistrationId("cognito").getClientId())
                .username(email)
                .secretHash(secretHash(email))
                .build());
    }

    public Tokens login(LoginRequest login) {
        InitiateAuthResponse result = cognito.initiateAuth(InitiateAuthRequest.builder()
                .clientId(registrations.findByRegistrationId("cognito").getClientId())
                .authFlow(AuthFlowType.USER_PASSWORD_AUTH)
                .authParameters(Map.of(
                        "USERNAME", login.email(),
                        "PASSWORD", login.password(),
                        "SECRET_HASH", secretHash(login.email())))
                .build());

        AuthenticationResultType tokens = result.authenticationResult();
        if (tokens == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This account needs an additional step. 5dm b Cognito login button.");
        }

        Jwt identity = jwtService.decodeIdToken(tokens.idToken());
        try {
            cognitoUserService.resolveOrCreate(identity.getSubject(),
                    identity.getClaimAsString("email"), identity.getClaimAsString("preferred_username"));
        } catch (OAuth2AuthenticationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
        }
        return new Tokens(tokens.accessToken(), tokens.refreshToken());
    }

    public AuthResponse profile(String token) {
        Jwt jwt = jwtService.decodeAccessToken(token);
        User user = users.findByCognitoSub(jwt.getSubject())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Local profile not found"));
        return new AuthResponse(user.getId(), user.getUsername());
    }

    public long accessTokenMaxAge(String token) {
        return Math.max(0, Duration.between(Instant.now(),
                jwtService.decodeAccessToken(token).getExpiresAt()).getSeconds());
    }

    public Tokens refresh(String refreshToken) {
        if (refreshToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        }
        ClientRegistration registration = registrations.findByRegistrationId("cognito");
        try {
            GetTokensFromRefreshTokenResponse result = cognito.getTokensFromRefreshToken(
                    GetTokensFromRefreshTokenRequest.builder()
                            .clientId(registration.getClientId())
                            .clientSecret(registration.getClientSecret())
                            .refreshToken(refreshToken)
                            .build());
            AuthenticationResultType tokens = result.authenticationResult();
            return new Tokens(tokens.accessToken(), tokens.refreshToken());
        } catch (NotAuthorizedException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cognito rejected refresh; sign in again");
        }
    }

    public void logout(String refreshToken) {
        if (refreshToken == null) return;
        ClientRegistration registration = registrations.findByRegistrationId("cognito");
        cognito.revokeToken(RevokeTokenRequest.builder()
                .clientId(registration.getClientId())
                .clientSecret(registration.getClientSecret())
                .token(refreshToken)
                .build());
    }


    private String secretHash(String username) {
        ClientRegistration registration = registrations.findByRegistrationId("cognito");
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(registration.getClientSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(
                    (username + registration.getClientId()).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot calculate Cognito client secret hash", ex);
        }
    }
}

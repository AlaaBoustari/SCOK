package org.langost.scok.config.tokens;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final NimbusJwtDecoder decoder;
    private final NimbusJwtDecoder idDecoder;
    private final String clientId;

    public JwtService(
            @Value("${spring.security.oauth2.client.provider.cognito.issuer-uri}") String issuer,
            @Value("${spring.security.oauth2.client.registration.cognito.client-id}") String clientId) {
        this.clientId = clientId;

        decoder = NimbusJwtDecoder.withIssuerLocation(issuer).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), this::validateAccessClaims));

        idDecoder = NimbusJwtDecoder.withJwkSetUri(issuer + "/.well-known/jwks.json").build();
        idDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(issuer), this::validateIdClaims));
    }

    private OAuth2TokenValidatorResult validateAccessClaims(Jwt jwt) {
        boolean accessToken = "access".equals(jwt.getClaimAsString("token_use"));
        boolean ourClient = clientId.equals(jwt.getClaimAsString("client_id"));
        if (accessToken && ourClient) return OAuth2TokenValidatorResult.success();
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(
                "invalid_token", "Expected a Cognito access token for this app.", null));
    }

    private OAuth2TokenValidatorResult validateIdClaims(Jwt jwt) {
        boolean idToken = "id".equals(jwt.getClaimAsString("token_use"));
        boolean ourClient = jwt.getAudience().contains(clientId);
        if (idToken && ourClient) return OAuth2TokenValidatorResult.success();
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
    }

    public Jwt decodeAccessToken(String token) {
        return decoder.decode(token);
    }

    public Jwt decodeIdToken(String token) {
        return idDecoder.decode(token);
    }

}

package org.langost.scok.aspect;

import org.langost.scok.controller.AuthController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CognitoIdentityProviderException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.NotAuthorizedException;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotConfirmedException;

import java.util.Map;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> error(ResponseStatusException ex) {
        String message = ex.getReason();
        if (message == null) message = "Authentication failed";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", message));
    }

    @ExceptionHandler(CognitoIdentityProviderException.class)
    public ResponseEntity<Map<String, String>> cognitoError(CognitoIdentityProviderException ex) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String message = ex.awsErrorDetails() == null ? "Cognito request failed" : ex.awsErrorDetails().errorMessage();
        if (ex instanceof UserNotConfirmedException) {
            status = HttpStatus.CONFLICT;
            message = "Confirm your email before signing in.";
        } else if (ex instanceof NotAuthorizedException) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (ex.statusCode() >= 500) {
            status = HttpStatus.BAD_GATEWAY;
        }
        if (message == null) message = "Cognito request failed";
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<Map<String, String>> sdkError(SdkClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("message", "Cognito is unavailable; try again"));
    }
}

package org.langost.scok.service;

import lombok.RequiredArgsConstructor;
import org.langost.scok.entity.User;
import org.langost.scok.repository.UserRepository;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CognitoUserService {

    private final UserRepository userRepository;


    @Transactional
    public User resolveOrCreate(OidcUser identity) {
        String username = identity.getClaimAsString("preferred_username");
        if (!StringUtils.hasText(username)) username = identity.getFullName();
        return resolveOrCreate(identity.getSubject(), identity.getEmail(), username);
    }

    @Transactional
    public User resolveOrCreate(String sub, String email, String username) {
        User existing = userRepository.findByCognitoSub(sub).orElse(null);
        if (existing != null) return existing;
        return createUser(sub, email, username);
    }

    private User createUser(String sub, String email, String username) {
        if (!StringUtils.hasText(email)) {
            throw authenticationError("Cognito did not supply an email.");
        }

        if (userRepository.existsByEmail(email)) {
            throw authenticationError(
                    "An existing local account uses this email. "
                            + "Account linking is required."
            );
        }

        if (!StringUtils.hasText(username)) {
            username = "user-" + sub;
        }

        User user = new User();
        user.setCognitoSub(sub);
        user.setEmail(email);
        user.setUsername(username);

        return userRepository.save(user);
    }

    private OAuth2AuthenticationException authenticationError(
            String message) {

        return new OAuth2AuthenticationException(
                new OAuth2Error("local_profile_error"),
                message
        );
    }
}

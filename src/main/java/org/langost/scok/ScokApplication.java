package org.langost.scok;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

@SpringBootApplication
public class ScokApplication {

    @Bean(destroyMethod = "close")
    public CognitoIdentityProviderClient cognitoClient(@Value("${app.aws.region:eu-west-1}") String region) {
        // User signup/login APIs use the Cognito app client, not IAM access keys.
        return CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(ScokApplication.class, args);
    }

}

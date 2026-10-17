# New concepts Learned : 

# Everything from Telusko's SpringSecurity playlist
    
    - Setting up SpringSecurity ( filtreChain - DaoProvider - Authentification Manager )
    - HomeMade JWT provider , verifier and claimExtractor.
    - When by LoadByUserName , JwtFilter , UserPrincipal Class.
    - Summary syntax :
        if (jwtService.isTokenValid(token)) {
            String email = jwtService.extractEmail(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);
        }

    - https://www.youtube.com/watch?v=Zxwq3aW9ctU&list=PLsyeobzWxl7qbKoSgR5ub6jolI8-ocxCF

# You can use issue both Refresh Tokens and access Tokens

    they keep the user logged in when the JWT expires by exposing a /refresh.
    note : client makes a normal API call with the access token. 
    If the server responds 401, the client's HTTP layer catches that specific case, 
    calls /refresh with the stored refresh token, gets a new access token, 
    retries the original failed request ( INTERCEPTOR LIKE AXIOS )



# You shouldn't put /refresh under permitAll umbrella since my refresh service relies on the JWT token. 

# You can use a RECORD instead of creating a class from scratch since DTOs are immutable

    public record LoginRequest(@Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password) {}

# You can use Response entity for better HTTP responses and to Better use Response and request DTOs even VOID return type

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest){
    return ResponseEntity.ok(authService.signup(signUpRequest));
    }


    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal principal){
    authService.logout(principal.getUser());
    return ResponseEntity.noContent().build();
    }

# You can use the UserPrincipal Class to avoid DB queries in the controller again and get the current Authenticated User

    public class UserPrincipal implements UserDetails 

# You can use Boolen exists by X isntead of pulling the entier entity just to use IsPresent()

    if(userRepository.existsByEmail(signUpRequest.email())){

# You can add this line to see Spring security debugs 

    logging.level.org.springframework.security=DEBUG

# You can, If Maven cannot find Lombok's annotation processor, explicitly configure it in maven-compiler-plugin

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.46</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>



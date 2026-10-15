



# SIGN_UP_SYNTAX:

```java
public boolean signup(SignUpRequest signup) {
var payload = accountRequest(signup.email());
payload.put("Password", signup.password());
payload.put("UserAttributes", List.of(
Map.of("Name", "email", "Value", signup.email()),
Map.of("Name", "preferred_username", "Value", signup.username())));
return Boolean.TRUE.equals(cognito("SignUp", payload).get("UserConfirmed"));
}

```

    https://docs.aws.amazon.com/cli/latest/reference/cognito-idp/admin-update-user-attributes.html

    [
    {
    "Name": "string",
    "Value": "string"
    }
    ...
    ]

Var introduced in java 10 is used for variables inside methodes and loops to declare a variable whose 
data is gonna be interpreted automatically.


# Spring_Cookies:

```java
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
    @CookieValue(name = AuthService.REFRESH_COOKIE, required = false) String refreshToken,
    HttpServletRequest request, HttpServletResponse response) {
    requireBrowserRequest(request);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
    .body(authService.refresh(refreshToken, request, response));
    }
```

@CookieValue() Basically takes the incoming Cookie from the sender and stores it in a variable 
and CacheControl.noStore basically prevents the frontend from caching the response.






# Auth Helper Method:

```java
    private Map<String, Object> accountRequest(String email) {
    var payload = new HashMap<String, Object>();
    payload.put("ClientId", registrations.findByRegistrationId("cognito").getClientId());
    payload.put("Username", email);
    payload.put("SecretHash", secretHash(email));
    return payload;
}
```
All Requests to cognito require the (ClientId Username SecretHash) so for separation of concerns, we added
this helper function :) 




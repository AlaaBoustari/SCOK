# AUTH

<img width="5333" height="2694" alt="COGNITO" src="https://github.com/user-attachments/assets/9c0b73d9-5890-4569-bfcf-396720a1c1f1" />

## Backend-for-Frontend architecture : AWS_COGNITO

*Authentication with Cognito + Spring Security*

> The idea was to replace the DAO authentication with **Amazon Cognito**, but it quickly spiraled into debugging and reading Java documentation :(

The app has two different login methods: fully managed by AWS and a custom login backed by the Cognito API.

The BFF architecture is a security pattern where the server-side (SpringBoot in our case) handles token negotiation, expiry and validation on behalf of the client. This is achieved by letting the client store HttpOnly Cookies and send them to the server on each request. This prevents frontend JavaScript from reading the authentication cookies.Greatly limiting Cross-Site Scripting.


## METHOD UNO: Cloud-managed login

The idea here is to let Spring Security handle the authentication flow by exchanging the short-lived authorization code you get upon a successful authentication, through AWS managed interface, for access/refresh Tokens, Spring does this automatically by making a post request to the Cognito’s servers. Our code then handles either:

1. **Resolving or creating the user:** By using the recently created *OidcUser* to populate an identity variable and use it to map the Cognito authenticated user to our database’s User Entity.

2. **Completing the login:** By setting the HttpOnly Cookie containing the tokens and killing the JSESSIONID session identifier cookie, invalidating the temporary session created by spring, clearing the clients registry’s authorized_client entry holding the users info in memory and wiping the security context for that request.

<img width="4652" height="3940" alt="Cloud_Managed_path" src="https://github.com/user-attachments/assets/f8a962aa-f64b-4f47-b833-9ccac7e47e0f" />


---

## METHOD DOS: Custom login backed by the Cognito API

The idea here is to have our own form and fields and using the AWS SDK for Java 2.x with the BUILDER design pattern to build the API request mentioned here: https://docs.aws.amazon.com/cognito-user-identity-pools/latest/APIReference/Welcome.html

Then we send the request to Cognito's server, identifying our app with its client ID and authenticating the user with their credentials. Our app client also requires a secret hash.

On success, the AuthenticationResult we get contains multiple attributes such as the ID_Token that we decode using the methods provided by NimusDecoder to get the (the sub, id ,email…) in order to resolve the local user as well as THE  ACCESS AND REFRESH TOKENS that we send through the AuthController in the form of HttpCookie to the client. This flow does NOT require authentication but rather WHEN the frontend resends said cookie with each request it is later treated and validated by SpringSecurity.

<img width="5026" height="5826" alt="USERNAME_PASSWORD_AUTHPATH" src="https://github.com/user-attachments/assets/37eb9f48-f6c2-4566-bd45-6063626360b9" />

---

## Shared flows: CSRF and authorization

*While the flow of getting the tokens differs, once they are in HttpOnly cookies through `completeLogin()` or a POST to `/api/auth/login`, both flows converge.*

The client calls **`requireSession()`** to send a request to `/api/auth/me`. The backend uses the current authenticated principal to return the local user ID and username as JSON. If the access token is missing or expired, the stored refresh token is sent back as a cookie and passed upward to Cognito's servers. If it is still valid, we get a new access token, which is sent back as a cookie. The existing refresh cookie stays unless Cognito returns a rotated refresh token, in which case we replace it too. Otherwise, you have to sign in again. For an API request returning **401**, the client attempts refresh and retries the original request **once**.

### CSRF

Before state-changing requests, JavaScript obtains a CSRF token through **`csrfHeaders()`** and attaches the returned header. Spring Security's **`CsrfFilter`** validates it against the expected token stored in the CSRF cookie and rejects missing or invalid tokens.

<img width="5674" height="7204" alt="shared_flow" src="https://github.com/user-attachments/assets/a4909e08-86c1-46cc-9df1-5bb3a744615c" />

---

## AUTH synthesis

By applying a **Backend-for-Frontend architecture** that I `totally` knew before I implemented, we made our **API authentication STATELESS**, with server-side token validation and an authenticated WebSocket handshake upgrade using **Cognito-issued JWTs**.

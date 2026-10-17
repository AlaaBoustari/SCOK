# AUTH

<img width="5333" height="2694" alt="COGNITO" src="https://github.com/user-attachments/assets/9c0b73d9-5890-4569-bfcf-396720a1c1f1" />

## Backend-for-Frontend architecture : AWS_COGNITO

*Authentication with Cognito + Spring Security*

> The idea was to replace the DAO authentication with **Amazon Cognito**, but it quickly spiraled into debugging and reading Java documentation :(

The app has two different login methods: fully managed by AWS and a custom login backed by the Cognito API.

The BFF architecture is a security pattern where the server-side (SpringBoot in our case) handles token negotiation, expiry and validation on behalf of the client. This is achieved by letting the client store HttpOnly Cookies and send them to the server on each request. This prevents frontend JavaScript from reading the authentication cookies.Greatly limiting Cross-Site Scripting.

---
# ATTACHMENT PROCESSING

<img width="1634" height="918" alt="SQS+S3" src="https://github.com/user-attachments/assets/ce4d87b4-5d68-4406-9a03-3d08d8d8a1c5" />

# Claim Check Integration Pattern

*Storage and processing with S3 and SQS*

> The idea was to decouple image storage and thumbnail generation by employing S3 and SQS + DLQ.

Claim Check is an integration pattern used to handle large payloads over lightweight message queues (for example, SQS tops at 1 MiB). So, instead of passing a massive payload directly through a message queue, the system stores the full thing in an external data store and sends only a small reference, the **claim check**, through the message broker.

In our case, we store the message’s attachment in S3 and send an **“All object create events”** notification to SQS containing the bucket name and object key. Our Java worker “long-polls” the queue using Spring Cloud AWS, produces a thumbnail using Thumbnailator, updates the DB, and passes the acknowledgement so the queue can safely remove the message instead of passing it to the dead-letter queue.

# Attachment storage and processing with S3 and SQS

## Claim Check Pattern

> The idea was to decouple image storage and thumbnail generation by employing S3 and SQS + DLQ.

Claim Check is an integration pattern used to handle large payloads over lightweight message queues (for example, SQS tops at 1 MiB). So, instead of passing a massive payload directly through a message queue, the system stores the full thing in an external data store and sends only a small reference, the **claim check**, through the message broker.

In our case, we store the message’s attachment in S3 and send an **“All object create events”** notification to SQS containing the bucket name and object key. Our Java worker “long-polls” the queue using Spring Cloud AWS, produces a thumbnail using Thumbnailator, updates the DB, and passes the acknowledgement so the queue can safely remove the message instead of passing it to the dead-letter queue.


## Solution architecture

The S3 bucket for original images relies on a lifecycle policy to send images to the Infrequent Access storage class and then Glacier, following timings chosen based on expected messaging usage.
We use S3 presigned URLs so the browser can upload attachments directly to the private S3 bucket without passing the file through our backend or exposing AWS credentials.We also use temporary signed URLs to display thumbnails and open the original images without making either bucket public.

## Detailed sequence diagram

The API and thumbnail worker run in the same Spring Boot application. Once S3 accepts the upload, confirming and sending the chat attachment can happen independently of thumbnail processing.


## CCP synthesis

This pattern is extremely helpful to bypass broker size limits, reduce network congestion, and prevent memory bottlenecks on the broker. The pattern is an established industry integration pattern for solving **“The Large Message Problem,”** employed by companies like Netflix, Uber, and WhatsApp through tools like Apache Kafka.



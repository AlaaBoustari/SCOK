package org.langost.scok.service;

import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.langost.scok.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.JsonNode;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThumbnailWorker {
    private final S3Client s3Client;
    private final AttachmentRepository attachmentRepository;
    private final ObjectMapper json;

    @Value("${app.s3.bucket}")
    private String originalsBucket;

    @Value("${app.s3.thumbnail-bucket}")
    private String thumbnailBucket;

    @SqsListener(value = "${app.sqs.thumbnail-queue-url}", acknowledgementMode = "ON_SUCCESS",
            maxConcurrentMessages = "10", maxMessagesPerPoll = "10", pollTimeoutSeconds = "20")
    public void process(String body) throws IOException {
        var event = json.readTree(body);
        if ("s3:TestEvent".equals(event.path("Event").asText())) return;
        if (!event.has("Records")) throw new IllegalArgumentException("Expected an S3 event");

        for (var record : event.path("Records")) {
            processRecord(record);
        }
    }

    private void processRecord(JsonNode record) throws IOException {
        if (!record.path("eventName").asText().startsWith("ObjectCreated:")) return;
        String bucket = record.path("s3").path("bucket").path("name").asText();
        String key = URLDecoder.decode(record.path("s3").path("object").path("key").asText(),
                StandardCharsets.UTF_8);
        if (!originalsBucket.equals(bucket) || !key.startsWith("originals/rooms/")) return;

        processAttachment(bucket, key);
    }

    private void processAttachment(String bucket, String key) throws IOException {
        var attachment = attachmentRepository.findByFileKey(key).orElse(null);
        if (attachment == null || attachment.getThumbnailKey() != null) return;

        byte[] original = downloadOriginal(bucket, key);
        byte[] thumbnail = resizeImage(original);
        String thumbnailKey = "thumbnails/" + attachment.getId() + ".png";
        uploadThumbnail(thumbnail, thumbnailKey);
        attachmentRepository.updateThumbnailKey(attachment.getId(), thumbnailKey);
        log.info("Thumbnail ready for attachment {}", attachment.getId());
    }

    private byte[] downloadOriginal(String bucket, String key) {
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build())
                .asByteArray();
    }

    private byte[] resizeImage(byte[] original) throws IOException {
        var output = new ByteArrayOutputStream();
        Thumbnails.of(new ByteArrayInputStream(original))
                .size(320, 320)
                .outputFormat("png")
                .toOutputStream(output);
        return output.toByteArray();
    }

    private void uploadThumbnail(byte[] thumbnail, String thumbnailKey) {
        s3Client.putObject(PutObjectRequest.builder()
                .bucket(thumbnailBucket).key(thumbnailKey).contentType("image/png").build(),
                RequestBody.fromBytes(thumbnail));
    }
}

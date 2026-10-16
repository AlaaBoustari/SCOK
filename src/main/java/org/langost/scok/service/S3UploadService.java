package org.langost.scok.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {
    private final S3Presigner s3Presigner;
    private final RoomService roomService;

    @Value("${app.s3.bucket}")
    private String bucketName;

    public UploadResponse initiate(Long roomId, Long userId, String contentType) {
        roomService.getDetails(roomId, userId);

        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Only JPEG and PNG uploads are supported");
        };
        String fileKey = "originals/rooms/" + roomId + "/" + UUID.randomUUID() + extension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .putObjectRequest(putObjectRequest)
                .build();

        String uploadUrl = s3Presigner.presignPutObject(presignRequest).url().toString();
        return new UploadResponse(uploadUrl, fileKey);
    }

    public record UploadResponse(String uploadUrl, String fileKey) {}
}

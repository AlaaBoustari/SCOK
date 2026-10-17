package org.langost.scok.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.langost.scok.entity.Attachment;
import org.langost.scok.entity.Room;
import org.langost.scok.entity.User;
import org.langost.scok.repository.AttachmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class S3UploadService {
    private final S3Presigner s3Presigner;
    private final S3Client s3Client;
    private final RoomService roomService;
    private final AttachmentRepository attachmentRepository;

    @Value("${app.s3.bucket}")
    private String bucketName;

    @Value("${app.s3.thumbnail-bucket}")
    private String thumbnailBucket;

    public UploadResponse initiate(Long roomId, User uploader, String filename, String contentType) {
        Room room = roomService.getRoomForMember(roomId, uploader.getId());

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
        Attachment attachment = new Attachment();
        attachment.setRoom(room);
        attachment.setUploader(uploader);
        attachment.setFileKey(fileKey);
        attachment.setFilename(filename);
        attachment.setContentType(contentType);
        attachmentRepository.save(attachment);
        return new UploadResponse(uploadUrl, fileKey, attachment.getId());
    }

    public void complete(Long roomId, Long attachmentId, Long userId) {
        roomService.getRoomForMember(roomId, userId);
        Attachment attachment = attachmentRepository.findByIdAndRoomIdAndUploaderId(attachmentId, roomId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(attachment.getFileKey()).build());
        attachment.setUploaded(true);
        attachmentRepository.save(attachment);
    }

    public AttachmentUrls downloadUrl(Long roomId, Long attachmentId, Long userId) {
        roomService.getRoomForMember(roomId, userId);
        Attachment attachment = attachmentRepository.findByIdAndRoomIdAndUploadedTrue(attachmentId, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        String downloadUrl = presignedGet(bucketName, attachment.getFileKey());
        String thumbnailUrl = attachment.getThumbnailKey() == null
                ? null : presignedGet(thumbnailBucket, attachment.getThumbnailKey());
        return new AttachmentUrls(downloadUrl, thumbnailUrl);
    }

    private String presignedGet(String bucket, String key) {
        GetObjectPresignRequest request = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return s3Presigner.presignGetObject(request).url().toString();
    }

    public record UploadResponse(String uploadUrl, String fileKey, Long attachmentId) {}

    public record AttachmentUrls(String downloadUrl, String thumbnailUrl) {}
}

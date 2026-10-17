package org.langost.scok.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.langost.scok.entity.UserPrincipal;
import org.langost.scok.service.S3UploadService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/room/{roomId}/uploads")
public class UploadController {
    private final S3UploadService s3UploadService;

    @PostMapping
    public ResponseEntity<S3UploadService.UploadResponse> initiate(
            @PathVariable Long roomId,
            @Valid @RequestBody UploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(s3UploadService.initiate(roomId, principal.getUser(), request.filename(), request.contentType()));
    }

    @PostMapping("/{attachmentId}/complete")
    public ResponseEntity<Void> complete(
            @PathVariable Long roomId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        s3UploadService.complete(roomId, attachmentId, principal.getUser().getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attachmentId}/url")
    public ResponseEntity<S3UploadService.AttachmentUrls> downloadUrl(
            @PathVariable Long roomId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(s3UploadService.downloadUrl(roomId, attachmentId, principal.getUser().getId()));
    }

    public record UploadRequest(@NotBlank String filename, @NotBlank String contentType) {}
}

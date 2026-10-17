package org.langost.scok.dto.response;

import java.time.Instant;

public record MessageResponse(
        Long id,
        String content,
        Long senderId,
        String senderUsername,
        Long roomId,
        Instant sentAt,
        Long attachmentId,
        String attachmentFilename) {
}

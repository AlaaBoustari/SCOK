package org.langost.scok.repository;

import org.langost.scok.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    Optional<Attachment> findByFileKey(String fileKey);

    @Transactional
    @Modifying
    @Query("update Attachment a set a.thumbnailKey = :thumbnailKey where a.id = :id")
    void updateThumbnailKey(Long id, String thumbnailKey);

    Optional<Attachment> findByIdAndRoomIdAndUploaderId(Long id, Long roomId, Long uploaderId);

    Optional<Attachment> findByIdAndRoomIdAndUploadedTrue(Long id, Long roomId);
}

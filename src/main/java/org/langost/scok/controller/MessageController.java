package org.langost.scok.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.langost.scok.dto.request.SendMessageRequest;
import org.langost.scok.dto.response.MessageResponse;
import org.langost.scok.entity.UserPrincipal;
import org.langost.scok.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.security.Principal;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/room/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getRoomMessages(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                messageService.getRoomMessages(roomId, principal.getUser().getId()));
    }

    @MessageMapping("/rooms/{roomId}/messages")
    @SendTo("/topic/rooms/{roomId}")
    public MessageResponse sendMessage(
            @DestinationVariable Long roomId,
            Principal principal,
            @Valid @Payload SendMessageRequest request) {

        return messageService.sendMessage(
                roomId,
                principal.getName(),
                request.content(),
                request.attachmentId());

    }

}

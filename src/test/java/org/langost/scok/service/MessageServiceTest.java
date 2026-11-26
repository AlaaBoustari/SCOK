package org.langost.scok.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.langost.scok.entity.Message;
import org.langost.scok.entity.Room;
import org.langost.scok.entity.User;
import org.langost.scok.exceptions.NotMemberException;
import org.langost.scok.repository.AttachmentRepository;
import org.langost.scok.repository.MessageRepository;
import org.langost.scok.repository.RoomMembershipRepository;
import org.langost.scok.repository.RoomRepository;
import org.langost.scok.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MessageServiceTest {

    private static final long ROOM_ID = 10L;
    private static final long SENDER_ID = 20L;
    private static final String SENDER_EMAIL = "alex@example.com";

    private final MessageRepository messages = mock(MessageRepository.class);
    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMembershipRepository memberships = mock(RoomMembershipRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final MessageService service =
            new MessageService(messages, rooms, memberships, users, mock(AttachmentRepository.class));

    @BeforeEach
    void setUpRoomAndSender() {
        var room = new Room();
        room.setId(ROOM_ID);
        room.setIsActive(true);
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(room));

        var sender = new User();
        sender.setId(SENDER_ID);
        sender.setUsername("alex");
        when(users.findByEmail(SENDER_EMAIL)).thenReturn(Optional.of(sender));
    }

    @Test
    void savesTextMessageFromRoomMember() {
        long messageId = 30L;
        when(memberships.existsByRoomIdAndUserId(ROOM_ID, SENDER_ID)).thenReturn(true);
        when(messages.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            message.setId(messageId);
            return message;
        });

        var result = service.sendMessage(ROOM_ID, SENDER_EMAIL, "Hello!", null);

        assertEquals(messageId, result.id());
        assertEquals("Hello!", result.content());
        assertEquals(SENDER_ID, result.senderId());
        assertEquals(ROOM_ID, result.roomId());
    }

    @Test
    void rejectsMessageFromNonMemberWithoutSavingIt() {
        when(memberships.existsByRoomIdAndUserId(ROOM_ID, SENDER_ID)).thenReturn(false);

        assertThrows(NotMemberException.class,
                () -> service.sendMessage(ROOM_ID, SENDER_EMAIL, "Hello!", null));

        verifyNoInteractions(messages);
    }
}

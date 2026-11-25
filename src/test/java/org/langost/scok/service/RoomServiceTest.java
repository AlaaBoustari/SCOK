package org.langost.scok.service;

import org.junit.jupiter.api.Test;
import org.langost.scok.entity.Room;
import org.langost.scok.repository.RoomMembershipRepository;
import org.langost.scok.repository.RoomRepository;
import org.langost.scok.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoomServiceTest {

    private static final long ROOM_ID = 10L;
    private static final long USER_ID = 20L;

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final RoomMembershipRepository memberships = mock(RoomMembershipRepository.class);
    private final RoomService service =
            new RoomService(rooms, mock(UserRepository.class), memberships);

    @Test
    void rejectsNonMembers() {
        when(memberships.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).thenReturn(false);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.getRoomForMember(ROOM_ID, USER_ID));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
    }

    @Test
    void rejectsInactiveRooms() {
        var room = new Room();
        room.setIsActive(false);
        when(memberships.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).thenReturn(true);
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(room));

        var error = assertThrows(ResponseStatusException.class,
                () -> service.getRoomForMember(ROOM_ID, USER_ID));

        assertEquals(HttpStatus.GONE, error.getStatusCode());
    }

    @Test
    void returnsActiveRoomForMember() {
        var room = new Room();
        room.setIsActive(true);
        when(memberships.existsByRoomIdAndUserId(ROOM_ID, USER_ID)).thenReturn(true);
        when(rooms.findById(ROOM_ID)).thenReturn(Optional.of(room));

        assertSame(room, service.getRoomForMember(ROOM_ID, USER_ID));
    }
}
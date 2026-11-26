package org.langost.scok.service;

import org.junit.jupiter.api.Test;
import org.langost.scok.dto.request.RoomCreationRequest;
import org.langost.scok.entity.Room;
import org.langost.scok.entity.RoomMembership;
import org.langost.scok.entity.User;
import org.langost.scok.repository.RoomMembershipRepository;
import org.langost.scok.repository.RoomRepository;
import org.langost.scok.repository.UserRepository;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RoomServiceTest {

    private static final long ROOM_ID = 10L;
    private static final long USER_ID = 20L;

    private final RoomRepository rooms = mock(RoomRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final RoomMembershipRepository memberships = mock(RoomMembershipRepository.class);
    private final RoomService service = new RoomService(rooms, users, memberships);

    @Test
    void createsRoomAndAddsOwnerAsMember() {
        var owner = new User();
        owner.setId(USER_ID);
        owner.setUsername("alex");
        when(users.findById(USER_ID)).thenReturn(Optional.of(owner));
        when(rooms.save(any(Room.class))).thenAnswer(invocation -> {
            Room room = invocation.getArgument(0);
            room.setId(ROOM_ID);
            return room;
        });

        var result = service.create(new RoomCreationRequest("General"), USER_ID);

        assertEquals(ROOM_ID, result.id());
        assertEquals("General", result.name());
        assertEquals(USER_ID, result.ownerId());

        var membership = ArgumentCaptor.forClass(RoomMembership.class);
        verify(memberships).save(membership.capture());
        assertSame(owner, membership.getValue().getUser());
        assertEquals(ROOM_ID, membership.getValue().getRoom().getId());
    }

    @Test
    void rejectsDuplicateRoomNameForSameOwner() {
        when(rooms.existsByNameAndOwnerId("General", USER_ID)).thenReturn(true);

        var error = assertThrows(ResponseStatusException.class,
                () -> service.create(new RoomCreationRequest("General"), USER_ID));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(rooms, never()).save(any(Room.class));
        verifyNoInteractions(memberships);
    }

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

package org.langost.scok.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.langost.scok.dto.request.ChangeRoomNameRequest;
import org.langost.scok.dto.request.RoomCreationRequest;
import org.langost.scok.dto.response.RoomResponse;
import org.langost.scok.entity.Room;
import org.langost.scok.entity.RoomMembership;
import org.langost.scok.entity.User;
import org.langost.scok.repository.RoomMembershipRepository;
import org.langost.scok.repository.RoomRepository;
import org.langost.scok.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Transactional
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final RoomMembershipRepository roomMembershipRepository;


    public RoomResponse create(RoomCreationRequest request,Long ownerId){

        if(roomRepository.existsByNameAndOwnerId(request.roomName(),ownerId))
        {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"room deja exist");
        }
        User owner = userRepository.findById(ownerId)
                .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"User not Found"));

        Room room = new Room();
        room.setName(request.roomName());
        room.setOwner(owner);
        room = roomRepository.save(room);

        RoomMembership membership = new RoomMembership();
        membership.setUser(owner);
        membership.setRoom(room);
        roomMembershipRepository.save(membership);

        return toResponse(room);

    }

    public RoomResponse getDetails(Long roomId, Long currentUserId) {
        return toResponse(getRoomForMember(roomId, currentUserId));
    }

    public Room getRoomForMember(Long roomId, Long currentUserId) {
        if (!roomMembershipRepository.existsByRoomIdAndUserId(roomId, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not a member of this room");
        }
        return findActiveRoom(roomId);
    }

    public List<RoomResponse> getUserRooms(Long userId){
        return roomMembershipRepository.findRoomsByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();

    }

    public RoomResponse changeRoomName(Long roomId, ChangeRoomNameRequest request,Long currentUserId){

        Room room = roomRepository.findByIdAndOwnerId(roomId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not the owner of this room"));
        checkRoomIsActive(room);
        room.setName(request.newName());
        return toResponse(roomRepository.save(room));

    }

    public void deleteRoom(Long roomId,Long currentUserId){

        Room room = roomRepository.findByIdAndOwnerId(roomId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "You are not the owner of this room"));
        checkRoomIsActive(room);
        room.setIsActive(false);
        roomRepository.save(room);
    }

    private Room findActiveRoom(Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        checkRoomIsActive(room);
        return room;
    }

    private void checkRoomIsActive(Room room) {
        if (!room.getIsActive()) {
            throw new ResponseStatusException(HttpStatus.GONE, "Room is no longer active");
        }
    }

    private RoomResponse toResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getOwner().getId(),
                room.getOwner().getUsername(),
                room.getIsActive(),
                room.getCreatedAt()
        );
    }

}

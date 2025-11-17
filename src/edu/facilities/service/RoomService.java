package edu.facilities.service;

import edu.facilities.model.Room;
import edu.facilities.model.RoomType;
import edu.facilities.model.RoomStatus;

import java.util.ArrayList;
import java.util.List;

public class RoomService {

    private final List<Room> rooms = new ArrayList<>();

    public void createRoom(String id, String name, RoomType type, int capacity,
                           String location, RoomStatus status) {

        Room room = new Room(id, name, type, capacity, location, status);
        rooms.add(room);
    }

    public Room getRoomById(String roomId) {
        for (Room room : rooms) {
            if (room.getId().equals(roomId)) {
                return room;
            }
        }
        return null;
    }

    public List<Room> getAllRooms() {
        return new ArrayList<>(rooms);
    }

    public boolean deleteRoom(String roomId) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        return rooms.remove(room);
    }

    public boolean updateRoomType(String roomId, RoomType newType) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        room.setType(newType);
        return true;
    }

    public boolean updateRoomCapacity(String roomId, int newCapacity) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        room.setCapacity(newCapacity);
        return true;
    }

    public boolean updateRoomStatus(String roomId, RoomStatus newStatus) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        room.setStatus(newStatus);
        return true;
    }

    public List<Room> getAvailableRooms() {
        List<Room> availableRooms = new ArrayList<>();
        for (Room room : rooms) {
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }
}

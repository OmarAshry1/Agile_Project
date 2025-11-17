package edu.facilities.service;

import edu.facilities.model.Room;
import edu.facilities.model.RoomType;

import java.util.ArrayList;
import java.util.List;

public class RoomService {

    // Local in-memory list until database is added
    private final List<Room> rooms = new ArrayList<>();

    // --- CRUD Operations (Admin only for US 1.3) ---

    public void createRoom(String id, String name, RoomType type, int capacity, String location) {
        Room room = new Room(id, name, type, capacity, location);
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
        return new ArrayList<>(rooms);   // safe copy
    }

    public boolean deleteRoom(String roomId) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        // US 1.3 – must check no future bookings (placeholder)
        boolean hasFutureBookings = false; // Replace later with booking check

        if (hasFutureBookings) {
            return false; // cannot delete
        }

        return rooms.remove(room);
    }

    // --- Editing rooms (US 1.3) ---
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

    // Optionally allow editing name/location
    public boolean updateRoomName(String roomId, String newName) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        room.setName(newName);
        return true;
    }

    public boolean updateRoomLocation(String roomId, String newLocation) {
        Room room = getRoomById(roomId);
        if (room == null) return false;

        room.setLocation(newLocation);
        return true;
    }

    // --- US 1.1: Viewing Available Rooms ---
    public List<Room> getAvailableRooms() {
        List<Room> availableRooms = new ArrayList<>();
        for (Room room : rooms) {
            if (room.isAvailable()) {
                availableRooms.add(room);
            }
        }
        return availableRooms;
    }

    // Change availability (used later for booking)
    public void setAvailability(String roomId, boolean available) {
        Room room = getRoomById(roomId);
        if (room != null) {
            room.setAvailable(available);
        }
    }
}

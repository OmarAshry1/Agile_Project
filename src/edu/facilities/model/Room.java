package edu.facilities.model;

import java.util.Objects;

public class Room {

    private final String id;        // Unique ID (never changes)
    private String name;            // Editable? Optional if needed later
    private RoomType type;          // Editable according to US 1.3
    private int capacity;           // Editable according to US 1.3
    private String location;        // Usually fixed, but can be editable if needed

    private boolean available;      // Used for US 1.1 – real-time availability

    public Room(String id, String name, RoomType type, int capacity, String location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.location = location;
        this.available = true; // default
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public int getCapacity() { return capacity; }
    public String getLocation() { return location; }
    public boolean isAvailable() { return available; }

    // --- Setters for US 1.3 (Admin edits) ---
    public void setType(RoomType type) { this.type = type; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    // For debugging and UI
    @Override
    public String toString() {
        return name + " [" + id + "] (" + type + ", cap=" + capacity +
                ", loc=" + location + ", available=" + available + ")";
    }

    // Rooms are uniquely identified by their ID
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room)) return false;
        Room room = (Room) o;
        return Objects.equals(id, room.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

package edu.facilities.model;

import java.util.Objects;

public class Room {

    private final String id;
    private String name;
    private RoomType type;
    private int capacity;
    private String location;

    private boolean available;

    public Room(String id, String name, RoomType type, int capacity, String location) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.capacity = capacity;
        this.location = location;
        this.available = true;
    }

    // --- Getters ---
    public String getId() { return id; }
    public String getName() { return name; }
    public RoomType getType() { return type; }
    public int getCapacity() { return capacity; }
    public String getLocation() { return location; }
    public boolean isAvailable() { return available; }

    // --- Setters ---
    public void setType(RoomType type) { this.type = type; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public void setName(String name) { this.name = name; }
    public void setLocation(String location) { this.location = location; }

    public void setAvailable(boolean available) {
        this.available = available;
    }


    @Override
    public String toString() {
        return name + " [" + id + "] (" + type + ", cap=" + capacity +
                ", loc=" + location + ", available=" + available + ")";
    }


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

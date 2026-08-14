package edu.facilities.model;
public record CourseRoomAssignment(long id, Course course, Room room, String dayOfWeek, String startTime, String endTime) { }

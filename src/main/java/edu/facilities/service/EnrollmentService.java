package edu.facilities.service;
import edu.facilities.model.*; import java.sql.*;
public class EnrollmentService {
 public static final int MAX_CREDITS=18;
 public Enrollment enrollStudent(User student,Course course)throws SQLException{
  if(!"STUDENT".equals(student.getUserType())) throw new IllegalArgumentException("Only students can enroll.");
  if(course.getType() != CourseType.CORE) throw new IllegalArgumentException("This Sprint 1 registration workflow is for CORE courses only.");
  try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
   if(count(c,"select count(*) from enrollments where student_id=? and course_id=? and status='ENROLLED'",student.getId(),course.getId())>0) throw new IllegalArgumentException("You are already enrolled in this course.");
   if(count(c,"select count(*) from course_prerequisites p where p.course_id=? and not exists (select 1 from enrollments e where e.student_id=? and e.course_id=p.prerequisite_course_id and e.status='COMPLETED')",course.getId(),student.getId())>0) throw new IllegalArgumentException("Prerequisites are not completed.");
   try(PreparedStatement s=c.prepareStatement("select coalesce(sum(c.credits),0) from enrollments e join courses c on c.id=e.course_id where e.student_id=? and e.status='ENROLLED'")){s.setLong(1,Long.parseLong(student.getId()));try(ResultSet r=s.executeQuery()){r.next();if(r.getInt(1)+course.getCredits()>MAX_CREDITS)throw new IllegalArgumentException("Credit load cannot exceed "+MAX_CREDITS+" credits.");}}
   if(!course.hasAvailableSeats())throw new IllegalArgumentException("This course is full.");
   try(PreparedStatement s=c.prepareStatement("insert into enrollments(student_id,course_id) values(?,?) returning id,enrolled_at")){s.setLong(1,Long.parseLong(student.getId()));s.setLong(2,Long.parseLong(course.getId()));try(ResultSet r=s.executeQuery()){r.next();c.commit();return new Enrollment(String.valueOf(r.getLong(1)),student,course,r.getTimestamp(2).toLocalDateTime(),EnrollmentStatus.ENROLLED,null);}}
  }catch(RuntimeException|SQLException e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
 }
 private int count(Connection c,String sql,String a,String b)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,Long.parseLong(a));s.setLong(2,Long.parseLong(b));try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}}}
}

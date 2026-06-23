package com.smartcampus.enrolment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrolmentRecordRepository extends JpaRepository<EnrolmentRecord, Long> {
    
    // Checks if a student is already in a specific course (prevents double booking)
    boolean existsByStudentIdAndCourseId(String studentId, String courseId);
    
    // Fetches all courses a specific student is enrolled in
    List<EnrolmentRecord> findByStudentId(String studentId);
}
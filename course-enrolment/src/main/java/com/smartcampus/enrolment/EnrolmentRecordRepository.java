package com.smartcampus.enrolment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnrolmentRecordRepository extends JpaRepository<EnrolmentRecord, Long> {

    boolean existsByStudentIdAndCourseId(String studentId, String courseId);

    List<EnrolmentRecord> findByStudentId(String studentId);
}
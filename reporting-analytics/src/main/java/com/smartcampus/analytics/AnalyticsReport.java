package com.smartcampus.analytics;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_reports")
public class AnalyticsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime reportGeneratedAt;
    private int totalRegisteredStudents;
    private int totalCoursesOffered;
    
    public AnalyticsReport() {}

    public AnalyticsReport(int totalRegisteredStudents, int totalCoursesOffered) {
        this.reportGeneratedAt = LocalDateTime.now();
        this.totalRegisteredStudents = totalRegisteredStudents;
        this.totalCoursesOffered = totalCoursesOffered;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public LocalDateTime getReportGeneratedAt() { return reportGeneratedAt; }
    public int getTotalRegisteredStudents() { return totalRegisteredStudents; }
    public int getTotalCoursesOffered() { return totalCoursesOffered; }
}
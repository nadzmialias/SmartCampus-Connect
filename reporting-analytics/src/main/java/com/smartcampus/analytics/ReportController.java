package com.smartcampus.analytics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private AnalyticsRepository analyticsRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/generate")
    public ResponseEntity<?> generateNewReport() {
        try {
            // 1. Fetch all students from Profile Service (Port 8081)
            Object[] students = restTemplate.getForObject("http://localhost:8081/api/students", Object[].class);
            int studentCount = (students != null) ? students.length : 0;

            // 2. Fetch all courses from Enrolment Service (Port 8082)
            Object[] courses = restTemplate.getForObject("http://localhost:8082/api/enrol/courses", Object[].class);
            int courseCount = (courses != null) ? courses.length : 0;

            // 3. Save the aggregated data to the analytics database
            AnalyticsReport report = new AnalyticsReport(studentCount, courseCount);
            analyticsRepository.save(report);

            return new ResponseEntity<>(report, HttpStatus.CREATED);

        } catch (Exception e) {
            // Graceful Degradation (R9) - If Profile or Enrolment is offline, we handle it without crashing
            return new ResponseEntity<>("Error generating report: One or more upstream services are offline.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    @GetMapping
    public ResponseEntity<List<AnalyticsReport>> getAllReports() {
        return new ResponseEntity<>(analyticsRepository.findAll(), HttpStatus.OK);
    }
}
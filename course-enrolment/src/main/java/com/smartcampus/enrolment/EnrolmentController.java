package com.smartcampus.enrolment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/enrol")
public class EnrolmentController {

    private final JmsTemplate jmsTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Lock enrolLock = new ReentrantLock(); // Protects concurrency

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrolmentRecordRepository enrolmentRecordRepository;

    public EnrolmentController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @PostMapping
    public ResponseEntity<String> enrolStudent(@RequestBody EnrolmentRequest request) {
        enrolLock.lock();
        try {
            String studentId = request.getStudentId();
            String courseId = request.getCourseId();

            // Check Duplicate Enrolment
            if (enrolmentRecordRepository.existsByStudentIdAndCourseId(studentId, courseId)) {
                return new ResponseEntity<>("Enrolment failed: Student is already enrolled in this course.", HttpStatus.CONFLICT);
            }

            // Verify Profile via Student Service
            try {
                ResponseEntity<String> profileResponse = restTemplate.getForEntity(
                        "http://localhost:8081/api/students/" + studentId, String.class);
                        
            } catch (HttpClientErrorException e) {
                // This catches 4xx errors (like 404 Not Found) thrown by the Profile Service
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    return new ResponseEntity<>("Enrolment failed: Student ID not found in Profile Service.", HttpStatus.BAD_REQUEST);
                }
                return new ResponseEntity<>("Enrolment failed: Invalid request to Profile Service.", HttpStatus.BAD_REQUEST);
                
            } catch (ResourceAccessException e) {
                // This catches actual connection failures (i.e., Port 8081 is actually turned off)
                return new ResponseEntity<>("Enrolment failed: Profile Service offline.", HttpStatus.SERVICE_UNAVAILABLE);
                
            } catch (Exception e) {
                // Catch-all for anything else
                return new ResponseEntity<>("Enrolment failed: Unexpected system error.", HttpStatus.INTERNAL_SERVER_ERROR);
            }

            // Check Course Capacity
            Optional<Course> courseOpt = courseRepository.findById(courseId);
            if (courseOpt.isEmpty()) {
                return new ResponseEntity<>("Enrolment failed: Course does not exist.", HttpStatus.BAD_REQUEST);
            }
            Course course = courseOpt.get();
            if (course.getCapacity() <= 0) {
                return new ResponseEntity<>("Enrolment failed: Course is full.", HttpStatus.CONFLICT);
            }

            // Update Database
            course.setCapacity(course.getCapacity() - 1);
            courseRepository.save(course);
            enrolmentRecordRepository.save(new EnrolmentRecord(studentId, courseId));

            // Send JMS Notification
            jmsTemplate.convertAndSend("enrolmentQueue", "ENROLMENT_SUCCESS:" + studentId + ":" + courseId);

            return new ResponseEntity<>("Success: " + studentId + " enrolled in " + course.getName() + ".", HttpStatus.OK);
        } finally {
            enrolLock.unlock();
        }
    }

    @PostMapping("/courses")
    public ResponseEntity<String> createCatalogSubject(@RequestBody Course course) {
        if (courseRepository.existsById(course.getCourseId())) {
            return new ResponseEntity<>("Error: Course already exists.", HttpStatus.CONFLICT);
        }
        course.setCapacity(0); // Default to 0
        courseRepository.save(course);
        return new ResponseEntity<>("Catalog updated: " + course.getCourseId() + ".", HttpStatus.CREATED);
    }

    @PutMapping("/courses/{courseId}/seats")
    public ResponseEntity<String> allocateSeats(@PathVariable String courseId, @RequestParam int seatsToAdd) {
        Optional<Course> existingOpt = courseRepository.findById(courseId);
        if (existingOpt.isEmpty()) {
            return new ResponseEntity<>("Error: Target course not found in catalog.", HttpStatus.NOT_FOUND);
        }
        if (seatsToAdd <= 0) {
            return new ResponseEntity<>("Error: Must allocate at least 1 seat.", HttpStatus.BAD_REQUEST);
        }

        Course existing = existingOpt.get();
        int newTotal = existing.getCapacity() + seatsToAdd;
        existing.setCapacity(newTotal);
        courseRepository.save(existing);

        jmsTemplate.convertAndSend("enrolmentQueue", "SEAT_ALLOCATION:" + courseId + ":" + seatsToAdd + ":" + newTotal);

        return new ResponseEntity<>(String.format("Seats Allocated: %s now has %d total seats.", courseId, newTotal), HttpStatus.OK);
    }

    @GetMapping("/students/{studentId}/courses")
    public ResponseEntity<List<Course>> getStudentEnrolments(@PathVariable String studentId) {
        List<Course> enrolledCourses = enrolmentRecordRepository.findByStudentId(studentId).stream()
                .map(record -> courseRepository.findById(record.getCourseId()).orElse(null))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return new ResponseEntity<>(enrolledCourses, HttpStatus.OK);
    }

    @GetMapping("/courses")
    public ResponseEntity<List<Course>> getAllCourses() {
        return new ResponseEntity<>(courseRepository.findAll(), HttpStatus.OK);
    }

    @GetMapping("/courses/search")
    public ResponseEntity<List<Course>> searchCourses(@RequestParam String query) {
        String lowerQuery = query.toLowerCase();
        List<Course> results = courseRepository.findAll().stream()
                .filter(c -> c.getCourseId().toLowerCase().contains(lowerQuery) ||
                        c.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PutMapping("/courses/{courseId}")
    public ResponseEntity<String> updateCourse(@PathVariable String courseId, @RequestBody Course updatedCourse) {
        if (!courseRepository.existsById(courseId)) {
            return new ResponseEntity<>("Course not found.", HttpStatus.NOT_FOUND);
        }
        updatedCourse.setCourseId(courseId); // Prevent ID manipulation
        courseRepository.save(updatedCourse);
        return new ResponseEntity<>("Course updated.", HttpStatus.OK);
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<String> deleteCourse(@PathVariable String courseId) {
        if (courseRepository.existsById(courseId)) {
            courseRepository.deleteById(courseId);
            return new ResponseEntity<>("Course deleted.", HttpStatus.OK);
        }
        return new ResponseEntity<>("Course not found.", HttpStatus.NOT_FOUND);
    }

    public static class EnrolmentRequest {
        private String studentId;
        private String courseId;

        public String getStudentId() { return studentId; }
        public void setStudentId(String studentId) { this.studentId = studentId; }
        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
    }
}
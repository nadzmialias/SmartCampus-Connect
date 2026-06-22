package com.smartcampus.enrolment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/enrol")
public class EnrolmentController {

    private final JmsTemplate jmsTemplate;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Lock enrolLock = new ReentrantLock();

    private final Map<String, Course> courseDb = new ConcurrentHashMap<>();

    // NEW: A thread-safe ledger to remember who enrolled in what
    private final Set<String> enrolmentLedger = ConcurrentHashMap.newKeySet();

    public EnrolmentController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    // --- ENROLMENT TRANSACTION ENDPOINT ---
    @PostMapping
    public ResponseEntity<String> enrolStudent(@RequestBody EnrolmentRequest request) {
        enrolLock.lock();
        try {
            String studentId = request.getStudentId();
            String courseId = request.getCourseId();

            // NEW: 1. Check for Duplicate Enrolment FIRST (Saves processing time)
            String ledgerKey = studentId + "_" + courseId;
            if (enrolmentLedger.contains(ledgerKey)) {
                return new ResponseEntity<>("Enrolment failed: Student is already enrolled in this course.",
                        HttpStatus.CONFLICT);
            }

            // 2. Verify Profile
            try {
                ResponseEntity<String> profileResponse = restTemplate.getForEntity("http://localhost:8081/api/students",
                        String.class);
                if (!profileResponse.getBody().contains(studentId)) {
                    return new ResponseEntity<>("Enrolment failed: Student ID not found.", HttpStatus.BAD_REQUEST);
                }
            } catch (Exception e) {
                return new ResponseEntity<>("Enrolment failed: Profile Service offline.",
                        HttpStatus.SERVICE_UNAVAILABLE);
            }

            // 3. Check Course Data
            Course course = courseDb.get(courseId);
            if (course == null)
                return new ResponseEntity<>("Enrolment failed: Course does not exist.", HttpStatus.BAD_REQUEST);
            if (course.getCapacity() <= 0)
                return new ResponseEntity<>("Enrolment failed: Course is full.", HttpStatus.CONFLICT);

            // 4. Update & Notify
            course.setCapacity(course.getCapacity() - 1);
            enrolmentLedger.add(ledgerKey); // NEW: Record the successful enrolment in the ledger

            jmsTemplate.convertAndSend("enrolmentQueue", "ENROLMENT_SUCCESS:" + studentId + ":" + courseId);

            return new ResponseEntity<>("Success: " + studentId + " enrolled in " + course.getName() + ".",
                    HttpStatus.OK);
        } finally {
            enrolLock.unlock();
        }
    }

    // ==========================================
    // --- COURSE CRUDS ENDPOINTS ---
    // ==========================================

    // Create a new subject with 0 seats
    @PostMapping("/courses")
    public ResponseEntity<String> createCatalogSubject(@RequestBody Course course) {
        if (courseDb.containsKey(course.getCourseId())) {
            return new ResponseEntity<>("Error: Course already exists in the catalog.", HttpStatus.CONFLICT);
        }

        // Force the initial capacity to exactly 0 to prevent accidental allocations
        course.setCapacity(0);
        courseDb.put(course.getCourseId(), course);

        return new ResponseEntity<>("Catalog updated: " + course.getCourseId() + " created with 0 seats.",
                HttpStatus.CREATED);
    }

    // Allocate seats to an existing subject
    @PutMapping("/courses/{courseId}/seats")
    public ResponseEntity<String> allocateSeats(@PathVariable String courseId, @RequestParam int seatsToAdd) {
        Course existing = courseDb.get(courseId);

        if (existing == null) {
            return new ResponseEntity<>("Error: Target course not found in catalog.", HttpStatus.NOT_FOUND);
        }
        if (seatsToAdd <= 0) {
            return new ResponseEntity<>("Error: Must allocate at least 1 seat.", HttpStatus.BAD_REQUEST);
        }

        // Add the new seats to the existing capacity
        int newTotal = existing.getCapacity() + seatsToAdd;
        existing.setCapacity(newTotal);

        jmsTemplate.convertAndSend("enrolmentQueue", "SEAT_ALLOCATION:" + courseId + ":" + seatsToAdd + ":" + newTotal);

        return new ResponseEntity<>(String.format("Seats Allocated: %s now has %d total seats.", courseId, newTotal),
                HttpStatus.OK);
    }

    @GetMapping("/courses")
    public ResponseEntity<Collection<Course>> getAllCourses() {
        return new ResponseEntity<>(courseDb.values(), HttpStatus.OK);
    }

    @GetMapping("/courses/search")
    public ResponseEntity<Collection<Course>> searchCourses(@RequestParam String query) {
        String lowerQuery = query.toLowerCase();
        List<Course> results = courseDb.values().stream()
                .filter(c -> c.getCourseId().toLowerCase().contains(lowerQuery) ||
                        c.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    @PutMapping("/courses/{courseId}")
    public ResponseEntity<String> updateCourse(@PathVariable String courseId, @RequestBody Course updatedCourse) {
        Course existing = courseDb.get(courseId);
        if (existing == null)
            return new ResponseEntity<>("Course not found.", HttpStatus.NOT_FOUND);

        existing.setName(updatedCourse.getName());
        existing.setCapacity(updatedCourse.getCapacity());
        return new ResponseEntity<>("Course updated.", HttpStatus.OK);
    }

    @DeleteMapping("/courses/{courseId}")
    public ResponseEntity<String> deleteCourse(@PathVariable String courseId) {
        if (courseDb.remove(courseId) != null)
            return new ResponseEntity<>("Course deleted.", HttpStatus.OK);
        return new ResponseEntity<>("Course not found.", HttpStatus.NOT_FOUND);
    }

    // --- DTO TO CATCH JSON BODY ---
    public static class EnrolmentRequest {
        private String studentId;
        private String courseId;

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
        }

        public String getCourseId() {
            return courseId;
        }

        public void setCourseId(String courseId) {
            this.courseId = courseId;
        }
    }
}
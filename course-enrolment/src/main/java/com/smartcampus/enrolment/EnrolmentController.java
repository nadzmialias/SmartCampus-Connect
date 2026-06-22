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

            // Check for Duplicate Enrolment
            String studentCourses = studentId + "_" + courseId;
            if (enrolmentLedger.contains(studentCourses)) {
                return new ResponseEntity<>("Enrolment failed: Student is already enrolled in this course.",
                        HttpStatus.CONFLICT);
            }

            // Verify Profile
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

            // Check Course Data
            Course course = courseDb.get(courseId);
            if (course == null)
                return new ResponseEntity<>("Enrolment failed: Course does not exist.", HttpStatus.BAD_REQUEST);
            if (course.getCapacity() <= 0)
                return new ResponseEntity<>("Enrolment failed: Course is full.", HttpStatus.CONFLICT);

            // Update & Notify
            course.setCapacity(course.getCapacity() - 1);
            enrolmentLedger.add(studentCourses);

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
            return new ResponseEntity<>("Error: Course already exists.", HttpStatus.CONFLICT);
        }

        // Set the seat to 0 for new courses
        course.setCapacity(0);
        courseDb.put(course.getCourseId(), course);

        return new ResponseEntity<>("Catalog updated: " + course.getCourseId() + ".",
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

        // Add seats to the existing capacity
        int newTotal = existing.getCapacity() + seatsToAdd;
        existing.setCapacity(newTotal);

        jmsTemplate.convertAndSend("enrolmentQueue", "SEAT_ALLOCATION:" + courseId + ":" + seatsToAdd + ":" + newTotal);

        return new ResponseEntity<>(String.format("Seats Allocated: %s now has %d total seats.", courseId, newTotal),
                HttpStatus.OK);
    }

    // CHECK STUDENT ENROLMENT
    @GetMapping("/students/{studentId}/courses")
    public ResponseEntity<List<Course>> getStudentEnrolments(@PathVariable String studentId) {
        // Filter the ledger for this specific student's ID
        List<Course> enrolledCourses = enrolmentLedger.stream()
                .filter(entry -> entry.startsWith(studentId + "_"))
                .map(entry -> {
                    // Extract the Course ID from the "StudentId_CourseId" string
                    String courseId = entry.split("_")[1];
                    return courseDb.get(courseId); // Fetch the full course object
                })
                .filter(java.util.Objects::nonNull) // Failsafe in case a course was deleted
                .collect(Collectors.toList());

        return new ResponseEntity<>(enrolledCourses, HttpStatus.OK);
    }

    // SHOW ALL COURSES
    @GetMapping("/courses")
    public ResponseEntity<Collection<Course>> getAllCourses() {
        return new ResponseEntity<>(courseDb.values(), HttpStatus.OK);
    }

    // SEARCH COURSES (By ID or Name)
    @GetMapping("/courses/search")
    public ResponseEntity<Collection<Course>> searchCourses(@RequestParam String query) {
        String lowerQuery = query.toLowerCase();
        List<Course> results = courseDb.values().stream()
                .filter(c -> c.getCourseId().toLowerCase().contains(lowerQuery) ||
                        c.getName().toLowerCase().contains(lowerQuery))
                .collect(Collectors.toList());
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // UPDATE COURSE DETAILS (Name & Capacity)
    @PutMapping("/courses/{courseId}")
    public ResponseEntity<String> updateCourse(@PathVariable String courseId, @RequestBody Course updatedCourse) {
        Course existing = courseDb.get(courseId);
        if (existing == null)
            return new ResponseEntity<>("Course not found.", HttpStatus.NOT_FOUND);

        existing.setName(updatedCourse.getName());
        existing.setCapacity(updatedCourse.getCapacity());
        return new ResponseEntity<>("Course updated.", HttpStatus.OK);
    }

    // DELETE COURSE
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
package com.smartcampus.enrolment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/enrol")
public class EnrolmentController {

    @Autowired
    private JmsTemplate jmsTemplate;

    public EnrolmentController(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    // R4: Web Client to call the Profile Service
    private final RestTemplate restTemplate = new RestTemplate();

    // R5: Thread Pool to handle 10 concurrent requests simultaneously
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    // R5: Shared Mutable State (Class Capacities) and Lock to protect it
    private final ConcurrentHashMap<String, Integer> courseCapacities = new ConcurrentHashMap<>();
    private final ReentrantLock capacityLock = new ReentrantLock();

    // 1. View all active courses and their remaining seats
    @GetMapping("/courses")
    public ResponseEntity<java.util.Map<String, Integer>> getAllCourses() {
        return new ResponseEntity<>(courseCapacities, HttpStatus.OK);
    }

    @PostMapping("/courses")
    public ResponseEntity<String> manageCourse(@RequestParam String courseId, @RequestParam int capacity) {

        if (capacity < 0) {
            return new ResponseEntity<>("Capacity cannot be negative.", HttpStatus.BAD_REQUEST);
        }

        courseCapacities.put(courseId, capacity);
        return new ResponseEntity<>(String.format("System Updated: Course %s now has %d seats.", courseId, capacity),
                HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<String> enrolStudent(@RequestParam String studentId, @RequestParam String courseId) {

        // --- REQUIREMENT R4: SERVICE COMPOSITION ---
        // Synchronously check if the student exists in the Profile Service (Port 8081)
        try {
            String profileUrl = "http://localhost:8081/api/students/" + studentId;
            restTemplate.getForObject(profileUrl, Object.class);
        } catch (HttpClientErrorException.NotFound e) {
            return new ResponseEntity<>("Enrolment Rejected: Student ID not found in Profile Service.",
                    HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>("Enrolment Service Error: Profile Service is offline.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        // --- REQUIREMENT R5: MULTITHREADING & LOCKS ---
        // Offload the actual enrolment process to the background thread pool
        threadPool.submit(() -> {

            capacityLock.lock(); // LOCK: Prevent race conditions! No other thread can enter this block.
            try {
                int availableSeats = courseCapacities.getOrDefault(courseId, 0);

                if (availableSeats > 0) {
                    courseCapacities.put(courseId, availableSeats - 1);
                    String msg = String.format("SUCCESS: Student %s enrolled in %s. Seats left: %d", studentId,
                            courseId, availableSeats - 1);
                    jmsTemplate.convertAndSend("enrolment-queue", msg);
                } else {
                    String msg = String.format("FAILED: Student %s attempted to enrol. Class %s is FULL.", studentId,
                            courseId);
                    jmsTemplate.convertAndSend("enrolment-queue", msg);
                }
            } finally {
                capacityLock.unlock(); // UNLOCK: Always release the lock in a finally block!
            }
        });

        return new ResponseEntity<>("Enrolment request received and queued for processing.", HttpStatus.ACCEPTED);
    }
}
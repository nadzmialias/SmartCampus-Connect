package com.smartcampus.profile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    // CREATE
    @PostMapping
    public ResponseEntity<Student> registerStudent(@RequestBody Student student) {
        if (studentRepository.existsById(student.getStudentId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Student with ID " + student.getStudentId() + " already exists.");
        }
        Student savedStudent = studentRepository.save(student);
        return new ResponseEntity<>(savedStudent, HttpStatus.CREATED);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        return new ResponseEntity<>(studentRepository.findAll(), HttpStatus.OK);
    }

    // READ SINGLE (By ID in URL)
    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable String id) {
        Optional<Student> student = studentRepository.findById(id);
        return student.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // SEARCH (By ID or Name)
    @GetMapping("/search")
    public ResponseEntity<List<Student>> searchStudents(@RequestParam String query) {
        List<Student> results = studentRepository.findByNameContainingIgnoreCaseOrStudentIdContainingIgnoreCase(query,
                query);
        return new ResponseEntity<>(results, HttpStatus.OK);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<String> updateStudent(@PathVariable String id, @RequestBody Student updatedStudent) {
        if (!studentRepository.existsById(id)) {
            return new ResponseEntity<>("Student not found in database.", HttpStatus.NOT_FOUND);
        }
        // Ensure the ID isn't accidentally changed during update
        updatedStudent.setStudentId(id);
        studentRepository.save(updatedStudent);
        return new ResponseEntity<>("Profile updated successfully.", HttpStatus.OK);
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable String id) {
        if (studentRepository.existsById(id)) {
            studentRepository.deleteById(id);
            return new ResponseEntity<>("Student record completely removed.", HttpStatus.OK);
        }
        return new ResponseEntity<>("Student not found.", HttpStatus.NOT_FOUND);
    }
}
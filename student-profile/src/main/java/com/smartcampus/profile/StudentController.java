package com.smartcampus.profile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*") // Crucial for our dashboard
@RequestMapping("/api/students")
public class StudentController {

  // Our in-memory database
  private final Map<String, Student> studentDb = new ConcurrentHashMap<>();

  // 1. CREATE
  @PostMapping
  public ResponseEntity<Student> registerStudent(@RequestBody Student student) {
    studentDb.put(student.getStudentId(), student);
    return new ResponseEntity<>(student, HttpStatus.CREATED);
  }

  // 2. READ ALL
  @GetMapping
  public ResponseEntity<Collection<Student>> getAllStudents() {
    return new ResponseEntity<>(studentDb.values(), HttpStatus.OK);
  }

  // 6. READ SINGLE (By ID in URL)
  @GetMapping("/{id}")
  public ResponseEntity<Student> getStudentById(@PathVariable String id) {
    if (studentDb.containsKey(id)) {
      return new ResponseEntity<>(studentDb.get(id), HttpStatus.OK);
    }
    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
  }

  // 3. SEARCH (By ID or Name)
  @GetMapping("/search")
  public ResponseEntity<Collection<Student>> searchStudents(@RequestParam String query) {
    String lowerQuery = query.toLowerCase();
    List<Student> results = studentDb.values().stream()
        .filter(s -> s.getStudentId().toLowerCase().contains(lowerQuery) ||
            s.getName().toLowerCase().contains(lowerQuery))
        .collect(Collectors.toList());
    return new ResponseEntity<>(results, HttpStatus.OK);
  }

  // 4. UPDATE
  @PutMapping("/{id}")
  public ResponseEntity<String> updateStudent(@PathVariable String id, @RequestBody Student updatedStudent) {
    if (!studentDb.containsKey(id)) {
      return new ResponseEntity<>("Student not found in database.", HttpStatus.NOT_FOUND);
    }
    studentDb.put(id, updatedStudent);
    return new ResponseEntity<>("Profile updated successfully.", HttpStatus.OK);
  }

  // 5. DELETE
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteStudent(@PathVariable String id) {
    if (studentDb.remove(id) != null) {
      return new ResponseEntity<>("Student record completely removed.", HttpStatus.OK);
    }
    return new ResponseEntity<>("Student not found.", HttpStatus.NOT_FOUND);
  }
}
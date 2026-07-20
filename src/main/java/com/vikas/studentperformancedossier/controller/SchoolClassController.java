package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.dto.SchoolClassRequest;
import com.vikas.studentperformancedossier.dto.SchoolClassResponse;
import com.vikas.studentperformancedossier.service.SchoolClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/school-classes")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    public SchoolClassController(SchoolClassService schoolClassService) {
        this.schoolClassService = schoolClassService;
    }

    @GetMapping
    public List<SchoolClassResponse> getAllSchoolClasses() {
        return schoolClassService.findAll();
    }

    @GetMapping("/{id}")
    public SchoolClassResponse getSchoolClassById(@PathVariable Long id) {
        return schoolClassService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolClassResponse createSchoolClass(@Valid @RequestBody SchoolClassRequest request) {
        return schoolClassService.create(request);
    }

    @PutMapping("/{id}")
    public SchoolClassResponse updateSchoolClass(@PathVariable Long id, @Valid @RequestBody SchoolClassRequest request) {
        return schoolClassService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSchoolClass(@PathVariable Long id) {
        schoolClassService.delete(id);
    }
}

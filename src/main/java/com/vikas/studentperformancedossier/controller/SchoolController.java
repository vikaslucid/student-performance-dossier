package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.dto.SchoolRequest;
import com.vikas.studentperformancedossier.dto.SchoolResponse;
import com.vikas.studentperformancedossier.service.SchoolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/schools")
public class SchoolController {

    private final SchoolService schoolService;

    public SchoolController(SchoolService schoolService) {
        this.schoolService = schoolService;
    }

    @GetMapping
    public List<SchoolResponse> getAllSchools() {
        return schoolService.findAll();
    }

    @GetMapping("/{id}")
    public SchoolResponse getSchoolById(@PathVariable Long id) {
        return schoolService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public SchoolResponse createSchool(@Valid @RequestBody SchoolRequest request) {
        return schoolService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SchoolResponse updateSchool(@PathVariable Long id, @Valid @RequestBody SchoolRequest request) {
        return schoolService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteSchool(@PathVariable Long id) {
        schoolService.delete(id);
    }
}

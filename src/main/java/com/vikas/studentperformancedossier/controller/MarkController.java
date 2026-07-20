package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.dto.MarkRequest;
import com.vikas.studentperformancedossier.dto.MarkResponse;
import com.vikas.studentperformancedossier.service.MarkService;
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
@RequestMapping("/api/marks")
public class MarkController {

    private final MarkService markService;

    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @GetMapping
    public List<MarkResponse> getAllMarks() {
        return markService.findAll();
    }

    @GetMapping("/{id}")
    public MarkResponse getMarkById(@PathVariable Long id) {
        return markService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public MarkResponse createMark(@Valid @RequestBody MarkRequest request) {
        return markService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public MarkResponse updateMark(@PathVariable Long id, @Valid @RequestBody MarkRequest request) {
        return markService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMark(@PathVariable Long id) {
        markService.delete(id);
    }
}

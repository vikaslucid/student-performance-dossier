package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.dto.ExamRequest;
import com.vikas.studentperformancedossier.dto.ExamResponse;
import com.vikas.studentperformancedossier.service.ExamService;
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
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    public List<ExamResponse> getAllExams() {
        return examService.findAll();
    }

    @GetMapping("/{id}")
    public ExamResponse getExamById(@PathVariable Long id) {
        return examService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExamResponse createExam(@Valid @RequestBody ExamRequest request) {
        return examService.create(request);
    }

    @PutMapping("/{id}")
    public ExamResponse updateExam(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return examService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExam(@PathVariable Long id) {
        examService.delete(id);
    }
}

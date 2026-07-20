package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.ExamRequest;
import com.vikas.studentperformancedossier.dto.ExamResponse;
import com.vikas.studentperformancedossier.entity.Exam;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.ExamRepository;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import com.vikas.studentperformancedossier.repository.SubjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {

    private final ExamRepository examRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final SubjectRepository subjectRepository;

    public ExamService(ExamRepository examRepository, SchoolClassRepository schoolClassRepository,
                        SubjectRepository subjectRepository) {
        this.examRepository = examRepository;
        this.schoolClassRepository = schoolClassRepository;
        this.subjectRepository = subjectRepository;
    }

    public List<ExamResponse> findAll() {
        return examRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public ExamResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public ExamResponse create(ExamRequest request) {
        ensureUnique(request, null);
        SchoolClass schoolClass = findSchoolClassById(request.schoolClassId());
        Subject subject = findSubjectById(request.subjectId());
        Exam exam = new Exam();
        applyRequest(exam, request, schoolClass, subject);
        return toResponse(examRepository.save(exam));
    }

    public ExamResponse update(Long id, ExamRequest request) {
        ensureUnique(request, id);
        Exam existing = findEntityById(id);
        SchoolClass schoolClass = findSchoolClassById(request.schoolClassId());
        Subject subject = findSubjectById(request.subjectId());
        applyRequest(existing, request, schoolClass, subject);
        return toResponse(examRepository.save(existing));
    }

    public void delete(Long id) {
        examRepository.delete(findEntityById(id));
    }

    private void ensureUnique(ExamRequest request, Long excludingId) {
        examRepository.findBySchoolClass_IdAndSubject_IdAndName(
                        request.schoolClassId(), request.subjectId(), request.name())
                .filter(existing -> isDifferentRecord(existing, excludingId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "An exam named '" + request.name() + "' already exists for this school class and subject");
                });
    }

    private boolean isDifferentRecord(Exam existing, Long excludingId) {
        return excludingId == null || !existing.getId().equals(excludingId);
    }

    private Exam findEntityById(Long id) {
        return examRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Exam not found with id: " + id));
    }

    private SchoolClass findSchoolClassById(Long schoolClassId) {
        return schoolClassRepository.findById(schoolClassId)
                .orElseThrow(() -> new EntityNotFoundException("School class not found with id: " + schoolClassId));
    }

    private Subject findSubjectById(Long subjectId) {
        return subjectRepository.findById(subjectId)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found with id: " + subjectId));
    }

    private void applyRequest(Exam exam, ExamRequest request, SchoolClass schoolClass, Subject subject) {
        exam.setName(request.name());
        exam.setExamDate(request.examDate());
        exam.setSchoolClass(schoolClass);
        exam.setSubject(subject);
    }

    private ExamResponse toResponse(Exam exam) {
        return new ExamResponse(
                exam.getId(),
                exam.getName(),
                exam.getExamDate(),
                exam.getSchoolClass().getId(),
                exam.getSubject().getId(),
                exam.getCreatedAt(),
                exam.getUpdatedAt()
        );
    }
}

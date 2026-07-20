package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.SubjectRequest;
import com.vikas.studentperformancedossier.dto.SubjectResponse;
import com.vikas.studentperformancedossier.entity.Subject;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.SubjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubjectService {

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<SubjectResponse> findAll() {
        return subjectRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public SubjectResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public SubjectResponse create(SubjectRequest request) {
        ensureUnique(request, null);
        Subject subject = new Subject();
        applyRequest(subject, request);
        return toResponse(subjectRepository.save(subject));
    }

    public SubjectResponse update(Long id, SubjectRequest request) {
        ensureUnique(request, id);
        Subject existing = findEntityById(id);
        applyRequest(existing, request);
        return toResponse(subjectRepository.save(existing));
    }

    public void delete(Long id) {
        subjectRepository.delete(findEntityById(id));
    }

    private void ensureUnique(SubjectRequest request, Long excludingId) {
        subjectRepository.findByNameAndGradeLevel(request.name(), request.gradeLevel())
                .filter(existing -> isDifferentRecord(existing, excludingId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A subject named '" + request.name() + "' already exists for grade level '"
                                    + request.gradeLevel() + "'");
                });
    }

    private boolean isDifferentRecord(Subject existing, Long excludingId) {
        return excludingId == null || !existing.getId().equals(excludingId);
    }

    private Subject findEntityById(Long id) {
        return subjectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Subject not found with id: " + id));
    }

    private void applyRequest(Subject subject, SubjectRequest request) {
        subject.setName(request.name());
        subject.setGradeLevel(request.gradeLevel());
    }

    private SubjectResponse toResponse(Subject subject) {
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                subject.getGradeLevel(),
                subject.getCreatedAt(),
                subject.getUpdatedAt()
        );
    }
}

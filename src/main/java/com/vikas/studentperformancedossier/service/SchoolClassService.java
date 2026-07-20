package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.SchoolClassRequest;
import com.vikas.studentperformancedossier.dto.SchoolClassResponse;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.SchoolClassRepository;
import com.vikas.studentperformancedossier.repository.SchoolRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;

    public SchoolClassService(SchoolClassRepository schoolClassRepository, SchoolRepository schoolRepository) {
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
    }

    public List<SchoolClassResponse> findAll() {
        return schoolClassRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public SchoolClassResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public SchoolClassResponse create(SchoolClassRequest request) {
        ensureUnique(request, null);
        School school = findSchoolById(request.schoolId());
        SchoolClass schoolClass = new SchoolClass();
        applyRequest(schoolClass, request, school);
        return toResponse(schoolClassRepository.save(schoolClass));
    }

    public SchoolClassResponse update(Long id, SchoolClassRequest request) {
        ensureUnique(request, id);
        SchoolClass existing = findEntityById(id);
        School school = findSchoolById(request.schoolId());
        applyRequest(existing, request, school);
        return toResponse(schoolClassRepository.save(existing));
    }

    public void delete(Long id) {
        schoolClassRepository.delete(findEntityById(id));
    }

    private void ensureUnique(SchoolClassRequest request, Long excludingId) {
        schoolClassRepository.findBySchool_IdAndGradeAndSection(request.schoolId(), request.grade(), request.section())
                .filter(existing -> isDifferentRecord(existing, excludingId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A school class with grade '" + request.grade() + "' and section '" + request.section()
                                    + "' already exists for school id " + request.schoolId());
                });
    }

    private boolean isDifferentRecord(SchoolClass existing, Long excludingId) {
        return excludingId == null || !existing.getId().equals(excludingId);
    }

    private SchoolClass findEntityById(Long id) {
        return schoolClassRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School class not found with id: " + id));
    }

    private School findSchoolById(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .orElseThrow(() -> new EntityNotFoundException("School not found with id: " + schoolId));
    }

    private void applyRequest(SchoolClass schoolClass, SchoolClassRequest request, School school) {
        schoolClass.setGrade(request.grade());
        schoolClass.setSection(request.section());
        schoolClass.setSchool(school);
    }

    private SchoolClassResponse toResponse(SchoolClass schoolClass) {
        return new SchoolClassResponse(
                schoolClass.getId(),
                schoolClass.getGrade(),
                schoolClass.getSection(),
                schoolClass.getSchool().getId(),
                schoolClass.getCreatedAt(),
                schoolClass.getUpdatedAt()
        );
    }
}

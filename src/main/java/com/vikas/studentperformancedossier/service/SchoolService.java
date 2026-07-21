package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.SchoolRequest;
import com.vikas.studentperformancedossier.dto.SchoolResponse;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.repository.SchoolRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;

    public SchoolService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public List<SchoolResponse> findAll() {
        return schoolRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public SchoolResponse findById(Long id) {
        return toResponse(findEntityById(id));
    }

    public SchoolResponse create(SchoolRequest request) {
        School school = new School();
        applyRequest(school, request);
        return toResponse(schoolRepository.save(school));
    }

    public SchoolResponse update(Long id, SchoolRequest request) {
        School existing = findEntityById(id);
        applyRequest(existing, request);
        return toResponse(schoolRepository.save(existing));
    }

    public void delete(Long id) {
        schoolRepository.delete(findEntityById(id));
    }

    private School findEntityById(Long id) {
        return schoolRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("School not found with id: " + id));
    }

    private void applyRequest(School school, SchoolRequest request) {
        school.setName(request.name());
        school.setAddress(request.address());
    }

    private SchoolResponse toResponse(School school) {
        return new SchoolResponse(
                school.getId(),
                school.getName(),
                school.getAddress(),
                school.getCreatedAt(),
                school.getUpdatedAt()
        );
    }
}

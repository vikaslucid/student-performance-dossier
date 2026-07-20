package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findBySchool_IdAndGradeAndSection(Long schoolId, String grade, String section);
}

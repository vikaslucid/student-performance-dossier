package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {

    Optional<SchoolClass> findBySchool_IdAndGradeAndStreamAndSection(Long schoolId, String grade, String stream, String section);

    List<SchoolClass> findByGradeIgnoreCaseAndStreamIgnoreCase(String grade, String stream);

    List<SchoolClass> findByGradeIgnoreCaseAndStreamIsNull(String grade);
}

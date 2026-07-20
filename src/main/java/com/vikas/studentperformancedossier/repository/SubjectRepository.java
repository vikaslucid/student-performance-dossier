package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {

    Optional<Subject> findByNameAndGradeLevel(String name, String gradeLevel);
}

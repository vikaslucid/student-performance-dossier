package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    Optional<Exam> findBySchoolClass_IdAndSubject_IdAndName(Long schoolClassId, Long subjectId, String name);
}

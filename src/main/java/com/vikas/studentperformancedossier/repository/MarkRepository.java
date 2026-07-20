package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.Mark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarkRepository extends JpaRepository<Mark, Long> {

    Optional<Mark> findByStudent_IdAndExam_Id(Long studentId, Long examId);

    List<Mark> findByStudent_Id(Long studentId);
}

package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByEmail(String email);

    Optional<Student> findByStudentNumber(String studentNumber);
}

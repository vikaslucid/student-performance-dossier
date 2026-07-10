package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
}

package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.School;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolRepository extends JpaRepository<School, Long> {
}

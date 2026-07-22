package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.entity.Behaviour;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BehaviourRepository extends JpaRepository<Behaviour, Long> {

    Optional<Behaviour> findByStudent_Id(Long studentId);
}

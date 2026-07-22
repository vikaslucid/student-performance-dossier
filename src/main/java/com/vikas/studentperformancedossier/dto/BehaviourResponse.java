package com.vikas.studentperformancedossier.dto;

import java.time.LocalDateTime;

public record BehaviourResponse(
        Long id,
        Integer attention,
        Integer participation,
        Integer discipline,
        Integer homeworkResponsibility,
        Integer communicationSkills,
        Integer confidence,
        Integer teamwork,
        Integer curiosity,
        Integer leadership,
        Integer criticalThinking,
        Integer overallBehaviour,
        String anecdotalObservation,
        String strength,
        String needsImprovement,
        String parentSupportRequired,
        Long studentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

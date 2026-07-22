package com.vikas.studentperformancedossier.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BehaviourRequest(
        @NotNull @Min(0) @Max(5) Integer attention,
        @NotNull @Min(0) @Max(5) Integer participation,
        @NotNull @Min(0) @Max(5) Integer discipline,
        @NotNull @Min(0) @Max(5) Integer homeworkResponsibility,
        @NotNull @Min(0) @Max(5) Integer communicationSkills,
        @NotNull @Min(0) @Max(5) Integer confidence,
        @NotNull @Min(0) @Max(5) Integer teamwork,
        @NotNull @Min(0) @Max(5) Integer curiosity,
        @NotNull @Min(0) @Max(5) Integer leadership,
        @NotNull @Min(0) @Max(5) Integer criticalThinking,
        @NotNull @Min(0) @Max(5) Integer overallBehaviour,
        String anecdotalObservation,
        String strength,
        String needsImprovement,
        String parentSupportRequired,
        @NotNull Long studentId
) {
}

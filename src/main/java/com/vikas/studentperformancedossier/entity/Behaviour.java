package com.vikas.studentperformancedossier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

// One Behaviour record per student (mirrors the real-world PTM dossier system, which tracks a
// single conduct profile per student rather than one per exam/term). Each dimension is rated
// 0-5, matching the scale already used for Mark components.
@Entity
@Table(name = "behaviours")
public class Behaviour extends BaseEntity {

    @Column(nullable = false)
    private Integer attention;

    @Column(nullable = false)
    private Integer participation;

    @Column(nullable = false)
    private Integer discipline;

    @Column(name = "homework_responsibility", nullable = false)
    private Integer homeworkResponsibility;

    @Column(name = "communication_skills", nullable = false)
    private Integer communicationSkills;

    @Column(nullable = false)
    private Integer confidence;

    @Column(nullable = false)
    private Integer teamwork;

    @Column(nullable = false)
    private Integer curiosity;

    @Column(nullable = false)
    private Integer leadership;

    @Column(name = "critical_thinking", nullable = false)
    private Integer criticalThinking;

    @Column(name = "overall_behaviour", nullable = false)
    private Integer overallBehaviour;

    @Column(name = "anecdotal_observation")
    private String anecdotalObservation;

    @Column
    private String strength;

    @Column(name = "needs_improvement")
    private String needsImprovement;

    @Column(name = "parent_support_required")
    private String parentSupportRequired;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private Student student;

    public Integer getAttention() {
        return attention;
    }

    public void setAttention(Integer attention) {
        this.attention = attention;
    }

    public Integer getParticipation() {
        return participation;
    }

    public void setParticipation(Integer participation) {
        this.participation = participation;
    }

    public Integer getDiscipline() {
        return discipline;
    }

    public void setDiscipline(Integer discipline) {
        this.discipline = discipline;
    }

    public Integer getHomeworkResponsibility() {
        return homeworkResponsibility;
    }

    public void setHomeworkResponsibility(Integer homeworkResponsibility) {
        this.homeworkResponsibility = homeworkResponsibility;
    }

    public Integer getCommunicationSkills() {
        return communicationSkills;
    }

    public void setCommunicationSkills(Integer communicationSkills) {
        this.communicationSkills = communicationSkills;
    }

    public Integer getConfidence() {
        return confidence;
    }

    public void setConfidence(Integer confidence) {
        this.confidence = confidence;
    }

    public Integer getTeamwork() {
        return teamwork;
    }

    public void setTeamwork(Integer teamwork) {
        this.teamwork = teamwork;
    }

    public Integer getCuriosity() {
        return curiosity;
    }

    public void setCuriosity(Integer curiosity) {
        this.curiosity = curiosity;
    }

    public Integer getLeadership() {
        return leadership;
    }

    public void setLeadership(Integer leadership) {
        this.leadership = leadership;
    }

    public Integer getCriticalThinking() {
        return criticalThinking;
    }

    public void setCriticalThinking(Integer criticalThinking) {
        this.criticalThinking = criticalThinking;
    }

    public Integer getOverallBehaviour() {
        return overallBehaviour;
    }

    public void setOverallBehaviour(Integer overallBehaviour) {
        this.overallBehaviour = overallBehaviour;
    }

    public String getAnecdotalObservation() {
        return anecdotalObservation;
    }

    public void setAnecdotalObservation(String anecdotalObservation) {
        this.anecdotalObservation = anecdotalObservation;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getNeedsImprovement() {
        return needsImprovement;
    }

    public void setNeedsImprovement(String needsImprovement) {
        this.needsImprovement = needsImprovement;
    }

    public String getParentSupportRequired() {
        return parentSupportRequired;
    }

    public void setParentSupportRequired(String parentSupportRequired) {
        this.parentSupportRequired = parentSupportRequired;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }
}

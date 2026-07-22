package com.vikas.studentperformancedossier.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

// Each component is scored 0-5 (Concept/Application/Accuracy/Homework/Test), matching the
// rubric this project's real-world dossier system already uses. Total (0-25), percentage and
// letter grade are derived from the components rather than stored, so they can never drift out
// of sync with them.
@Entity
@Table(name = "marks")
public class Mark extends BaseEntity {

    @Column(nullable = false)
    private Integer concept;

    @Column(nullable = false)
    private Integer application;

    @Column(nullable = false)
    private Integer accuracy;

    @Column(nullable = false)
    private Integer homework;

    @Column(nullable = false)
    private Integer test;

    @Column
    private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    public Integer getConcept() {
        return concept;
    }

    public void setConcept(Integer concept) {
        this.concept = concept;
    }

    public Integer getApplication() {
        return application;
    }

    public void setApplication(Integer application) {
        this.application = application;
    }

    public Integer getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Integer accuracy) {
        this.accuracy = accuracy;
    }

    public Integer getHomework() {
        return homework;
    }

    public void setHomework(Integer homework) {
        this.homework = homework;
    }

    public Integer getTest() {
        return test;
    }

    public void setTest(Integer test) {
        this.test = test;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Exam getExam() {
        return exam;
    }

    public void setExam(Exam exam) {
        this.exam = exam;
    }

    public int getTotal() {
        return concept + application + accuracy + homework + test;
    }

    public double getPercentage() {
        return getTotal() * 100.0 / 25.0;
    }

    public String getGrade() {
        double percentage = getPercentage();
        if (percentage >= 90) {
            return "A1";
        }
        if (percentage >= 80) {
            return "A2";
        }
        if (percentage >= 70) {
            return "B1";
        }
        if (percentage >= 60) {
            return "B2";
        }
        if (percentage >= 50) {
            return "C";
        }
        return "D";
    }
}

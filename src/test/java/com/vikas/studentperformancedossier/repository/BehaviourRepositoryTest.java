package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.Behaviour;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import com.vikas.studentperformancedossier.entity.Student;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class BehaviourRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BehaviourRepository behaviourRepository;

    @Test
    void findByStudentId_whenExists_returnsBehaviour() {
        Student student = persistedStudent();
        Behaviour behaviour = persistedBehaviour(student);

        Optional<Behaviour> found = behaviourRepository.findByStudent_Id(student.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(behaviour.getId());
    }

    @Test
    void findByStudentId_whenMissing_returnsEmpty() {
        Student student = persistedStudent();

        Optional<Behaviour> found = behaviourRepository.findByStudent_Id(student.getId());

        assertThat(found).isEmpty();
    }

    private SchoolClass persistedSchoolClass() {
        School school = new School();
        school.setName("Central High");
        school.setAddress("123 Main St");
        entityManager.persistAndFlush(school);

        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setGrade("Grade 10");
        schoolClass.setSection("A");
        schoolClass.setSchool(school);
        return entityManager.persistFlushFind(schoolClass);
    }

    private Student persistedStudent() {
        Student student = new Student();
        student.setFirstName("Ada");
        student.setLastName("Lovelace");
        student.setEmail("ada@example.com");
        student.setDateOfBirth(LocalDate.of(1990, 1, 1));
        student.setEnrollmentDate(LocalDate.of(2020, 1, 1));
        student.setStudentNumber("S-100");
        student.setSchoolClass(persistedSchoolClass());
        return entityManager.persistFlushFind(student);
    }

    private Behaviour persistedBehaviour(Student student) {
        Behaviour behaviour = new Behaviour();
        behaviour.setAttention(4);
        behaviour.setParticipation(4);
        behaviour.setDiscipline(4);
        behaviour.setHomeworkResponsibility(4);
        behaviour.setCommunicationSkills(4);
        behaviour.setConfidence(4);
        behaviour.setTeamwork(4);
        behaviour.setCuriosity(4);
        behaviour.setLeadership(4);
        behaviour.setCriticalThinking(4);
        behaviour.setOverallBehaviour(4);
        behaviour.setAnecdotalObservation("Participates actively in class discussions.");
        behaviour.setStudent(student);
        return entityManager.persistFlushFind(behaviour);
    }
}

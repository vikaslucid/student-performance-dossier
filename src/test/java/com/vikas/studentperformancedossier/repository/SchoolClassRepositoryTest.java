package com.vikas.studentperformancedossier.repository;

import com.vikas.studentperformancedossier.config.JpaAuditingConfig;
import com.vikas.studentperformancedossier.entity.School;
import com.vikas.studentperformancedossier.entity.SchoolClass;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class SchoolClassRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Test
    void findBySchoolIdAndGradeAndSection_whenExists_returnsSchoolClass() {
        School school = persistedSchool();
        SchoolClass schoolClass = persistedSchoolClass(school, "Grade 10", "A");

        Optional<SchoolClass> found = schoolClassRepository.findBySchool_IdAndGradeAndSection(
                school.getId(), "Grade 10", "A");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(schoolClass.getId());
    }

    @Test
    void findBySchoolIdAndGradeAndSection_whenMissing_returnsEmpty() {
        School school = persistedSchool();

        Optional<SchoolClass> found = schoolClassRepository.findBySchool_IdAndGradeAndSection(
                school.getId(), "Grade 99", "Z");

        assertThat(found).isEmpty();
    }

    @Test
    void findBySchoolIdAndGradeAndSection_whenSameGradeSectionDifferentSchool_returnsEmpty() {
        School school = persistedSchool("Central High");
        persistedSchoolClass(school, "Grade 10", "A");
        School otherSchool = persistedSchool("Other School");

        Optional<SchoolClass> found = schoolClassRepository.findBySchool_IdAndGradeAndSection(
                otherSchool.getId(), "Grade 10", "A");

        assertThat(found).isEmpty();
    }

    private School persistedSchool() {
        return persistedSchool("Central High");
    }

    private School persistedSchool(String name) {
        School school = new School();
        school.setName(name);
        school.setAddress("123 Main St");
        return entityManager.persistFlushFind(school);
    }

    private SchoolClass persistedSchoolClass(School school, String grade, String section) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setGrade(grade);
        schoolClass.setSection(section);
        schoolClass.setSchool(school);
        return entityManager.persistFlushFind(schoolClass);
    }
}

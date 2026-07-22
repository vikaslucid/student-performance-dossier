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

import java.util.List;
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
    void findBySchoolIdAndGradeAndStreamAndSection_whenExists_returnsSchoolClass() {
        School school = persistedSchool();
        SchoolClass schoolClass = persistedSchoolClass(school, "Grade 10", null, "A");

        Optional<SchoolClass> found = schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(
                school.getId(), "Grade 10", null, "A");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(schoolClass.getId());
    }

    @Test
    void findBySchoolIdAndGradeAndStreamAndSection_whenMissing_returnsEmpty() {
        School school = persistedSchool();

        Optional<SchoolClass> found = schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(
                school.getId(), "Grade 99", null, "Z");

        assertThat(found).isEmpty();
    }

    @Test
    void findBySchoolIdAndGradeAndStreamAndSection_whenSameGradeSectionDifferentSchool_returnsEmpty() {
        School school = persistedSchool("Central High");
        persistedSchoolClass(school, "Grade 10", null, "A");
        School otherSchool = persistedSchool("Other School");

        Optional<SchoolClass> found = schoolClassRepository.findBySchool_IdAndGradeAndStreamAndSection(
                otherSchool.getId(), "Grade 10", null, "A");

        assertThat(found).isEmpty();
    }

    @Test
    void findByGradeIgnoreCaseAndStreamIgnoreCase_matchesRegardlessOfCase() {
        School school = persistedSchool();
        SchoolClass schoolClass = persistedSchoolClass(school, "Eleventh", "ARTS", "A");

        List<SchoolClass> found = schoolClassRepository.findByGradeIgnoreCaseAndStreamIgnoreCase("ELEVENTH", "arts");

        assertThat(found).extracting(SchoolClass::getId).containsExactly(schoolClass.getId());
    }

    @Test
    void findByGradeIgnoreCaseAndStreamIsNull_matchesOnlyClassesWithoutStream() {
        School school = persistedSchool();
        SchoolClass withoutStream = persistedSchoolClass(school, "EIGHTH", null, "A");
        persistedSchoolClass(school, "Eleventh", "ARTS", "A");

        List<SchoolClass> found = schoolClassRepository.findByGradeIgnoreCaseAndStreamIsNull("eighth");

        assertThat(found).extracting(SchoolClass::getId).containsExactly(withoutStream.getId());
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

    private SchoolClass persistedSchoolClass(School school, String grade, String stream, String section) {
        SchoolClass schoolClass = new SchoolClass();
        schoolClass.setGrade(grade);
        schoolClass.setStream(stream);
        schoolClass.setSection(section);
        schoolClass.setSchool(school);
        return entityManager.persistFlushFind(schoolClass);
    }
}

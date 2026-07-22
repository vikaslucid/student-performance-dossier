package com.vikas.studentperformancedossier.service;

import com.vikas.studentperformancedossier.dto.BehaviourRequest;
import com.vikas.studentperformancedossier.dto.BehaviourResponse;
import com.vikas.studentperformancedossier.entity.Behaviour;
import com.vikas.studentperformancedossier.entity.Role;
import com.vikas.studentperformancedossier.entity.Student;
import com.vikas.studentperformancedossier.entity.User;
import com.vikas.studentperformancedossier.exception.DuplicateResourceException;
import com.vikas.studentperformancedossier.repository.BehaviourRepository;
import com.vikas.studentperformancedossier.repository.StudentRepository;
import com.vikas.studentperformancedossier.security.CurrentUserProvider;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BehaviourServiceTest {

    @Mock
    private BehaviourRepository behaviourRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private BehaviourService behaviourService;

    private BehaviourRequest request;

    @BeforeEach
    void setUp() {
        request = new BehaviourRequest(4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,
                "Participates actively.", "Good listener.", "Time management.", "None", 1L);
    }

    @Test
    void create_whenBehaviourAlreadyExistsForStudent_throwsDuplicateResourceException() {
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existingBehaviour(3L)));

        assertThatThrownBy(() -> behaviourService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("1");

        verify(behaviourRepository, never()).save(any());
    }

    @Test
    void create_whenStudentNotFound_throwsEntityNotFoundException() {
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> behaviourService.create(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");

        verify(behaviourRepository, never()).save(any());
    }

    @Test
    void create_whenNoDuplicatesAndStudentExists_savesBehaviour() {
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent(1L)));
        when(behaviourRepository.save(any(Behaviour.class))).thenReturn(existingBehaviour(1L));

        BehaviourResponse response = behaviourService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.attention()).isEqualTo(4);
        assertThat(response.overallBehaviour()).isEqualTo(4);
        assertThat(response.studentId()).isEqualTo(1L);
    }

    @Test
    void update_whenBehaviourBelongsToAnotherRecord_throwsDuplicateResourceException() {
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existingBehaviour(3L)));

        assertThatThrownBy(() -> behaviourService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(behaviourRepository, never()).findById(any());
        verify(behaviourRepository, never()).save(any());
    }

    @Test
    void update_whenBehaviourNotFound_throwsEntityNotFoundException() {
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());
        when(behaviourRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> behaviourService.update(99L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(behaviourRepository, never()).save(any());
    }

    @Test
    void update_whenBehaviourBelongsToSameRecord_updatesSuccessfully() {
        Behaviour self = existingBehaviour(1L);
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.of(self));
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(self));
        when(studentRepository.findById(1L)).thenReturn(Optional.of(existingStudent(1L)));
        when(behaviourRepository.save(any(Behaviour.class))).thenReturn(self);

        BehaviourResponse response = behaviourService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        verify(behaviourRepository).save(self);
    }

    @Test
    void delete_whenBehaviourNotFound_throwsEntityNotFoundException() {
        when(behaviourRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> behaviourService.delete(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(behaviourRepository, never()).delete(any());
    }

    @Test
    void delete_whenBehaviourExists_deletesBehaviour() {
        Behaviour existing = existingBehaviour(1L);
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(existing));

        behaviourService.delete(1L);

        verify(behaviourRepository).delete(existing);
    }

    @Test
    void findAll_whenAdmin_returnsAllBehaviours() {
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));
        when(behaviourRepository.findAll()).thenReturn(List.of(existingBehaviour(1L)));

        List<BehaviourResponse> responses = behaviourService.findAll();

        assertThat(responses).hasSize(1);
        verify(behaviourRepository, never()).findByStudent_Id(any());
    }

    @Test
    void findAll_whenTeacher_returnsAllBehaviours() {
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.TEACHER, null));
        when(behaviourRepository.findAll()).thenReturn(List.of(existingBehaviour(1L)));

        List<BehaviourResponse> responses = behaviourService.findAll();

        assertThat(responses).hasSize(1);
        verify(behaviourRepository, never()).findByStudent_Id(any());
    }

    @Test
    void findAll_whenStudent_returnsOnlyOwnBehaviour() {
        Student linkedStudent = existingStudent(1L);
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, linkedStudent));
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.of(existingBehaviour(1L)));

        List<BehaviourResponse> responses = behaviourService.findAll();

        assertThat(responses).hasSize(1);
        verify(behaviourRepository, never()).findAll();
    }

    @Test
    void findAll_whenStudentHasNoLinkedStudent_returnsEmptyList() {
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, null));

        List<BehaviourResponse> responses = behaviourService.findAll();

        assertThat(responses).isEmpty();
        verify(behaviourRepository, never()).findAll();
        verify(behaviourRepository, never()).findByStudent_Id(any());
    }

    @Test
    void findAll_whenStudentHasNoBehaviourRecordYet_returnsEmptyList() {
        Student linkedStudent = existingStudent(1L);
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, linkedStudent));
        when(behaviourRepository.findByStudent_Id(1L)).thenReturn(Optional.empty());

        List<BehaviourResponse> responses = behaviourService.findAll();

        assertThat(responses).isEmpty();
    }

    @Test
    void findById_whenStudentOwnsBehaviour_returnsBehaviour() {
        Student linkedStudent = existingStudent(1L);
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(existingBehaviour(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, linkedStudent));

        BehaviourResponse response = behaviourService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findById_whenStudentDoesNotOwnBehaviour_throwsEntityNotFoundException() {
        Student otherStudent = existingStudent(99L);
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(existingBehaviour(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, otherStudent));

        assertThatThrownBy(() -> behaviourService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("1");
    }

    @Test
    void findById_whenStudentHasNoLinkedStudent_throwsEntityNotFoundException() {
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(existingBehaviour(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.STUDENT, null));

        assertThatThrownBy(() -> behaviourService.findById(1L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void findById_whenAdmin_returnsAnyBehaviour() {
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(existingBehaviour(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.ADMIN, null));

        BehaviourResponse response = behaviourService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void findById_whenTeacher_returnsAnyBehaviour() {
        when(behaviourRepository.findById(1L)).thenReturn(Optional.of(existingBehaviour(1L)));
        when(currentUserProvider.getCurrentUser()).thenReturn(existingUser(Role.TEACHER, null));

        BehaviourResponse response = behaviourService.findById(1L);

        assertThat(response.id()).isEqualTo(1L);
    }

    private User existingUser(Role role, Student linkedStudent) {
        User user = new User();
        user.setId(1L);
        user.setUsername("ada");
        user.setPassword("hashed-password");
        user.setRole(role);
        user.setStudent(linkedStudent);
        return user;
    }

    private Behaviour existingBehaviour(Long id) {
        Behaviour behaviour = new Behaviour();
        behaviour.setId(id);
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
        behaviour.setAnecdotalObservation("Participates actively.");
        behaviour.setStudent(existingStudent(1L));
        return behaviour;
    }

    private Student existingStudent(Long id) {
        Student student = new Student();
        student.setId(id);
        student.setFirstName("Ada");
        student.setLastName("Lovelace");
        student.setEmail("ada@example.com");
        student.setDateOfBirth(LocalDate.of(1990, 1, 1));
        student.setEnrollmentDate(LocalDate.of(2020, 1, 1));
        student.setStudentNumber("S-100");
        return student;
    }
}

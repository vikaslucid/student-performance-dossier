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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BehaviourService {

    private final BehaviourRepository behaviourRepository;
    private final StudentRepository studentRepository;
    private final CurrentUserProvider currentUserProvider;

    public BehaviourService(BehaviourRepository behaviourRepository, StudentRepository studentRepository,
                             CurrentUserProvider currentUserProvider) {
        this.behaviourRepository = behaviourRepository;
        this.studentRepository = studentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public List<BehaviourResponse> findAll() {
        User currentUser = currentUserProvider.getCurrentUser();
        List<Behaviour> behaviours = currentUser.getRole() == Role.STUDENT
                ? findBehaviourForLinkedStudent(currentUser).map(List::of).orElse(List.of())
                : behaviourRepository.findAll();
        return behaviours.stream()
                .map(this::toResponse)
                .toList();
    }

    public BehaviourResponse findById(Long id) {
        Behaviour behaviour = findEntityById(id);
        User currentUser = currentUserProvider.getCurrentUser();

        if (currentUser.getRole() == Role.STUDENT && !belongsToLinkedStudent(behaviour, currentUser)) {
            // Same "not found" as a genuinely missing id - see MarkService.findById for why a
            // distinguishable 403 here would be an information leak.
            throw new EntityNotFoundException("Behaviour record not found with id: " + id);
        }

        return toResponse(behaviour);
    }

    private Optional<Behaviour> findBehaviourForLinkedStudent(User currentUser) {
        Student student = currentUser.getStudent();
        return student == null ? Optional.empty() : behaviourRepository.findByStudent_Id(student.getId());
    }

    private boolean belongsToLinkedStudent(Behaviour behaviour, User currentUser) {
        Student student = currentUser.getStudent();
        return student != null && behaviour.getStudent().getId().equals(student.getId());
    }

    public BehaviourResponse create(BehaviourRequest request) {
        ensureUnique(request, null);
        Student student = findStudentById(request.studentId());
        Behaviour behaviour = new Behaviour();
        applyRequest(behaviour, request, student);
        return toResponse(behaviourRepository.save(behaviour));
    }

    public BehaviourResponse update(Long id, BehaviourRequest request) {
        ensureUnique(request, id);
        Behaviour existing = findEntityById(id);
        Student student = findStudentById(request.studentId());
        applyRequest(existing, request, student);
        return toResponse(behaviourRepository.save(existing));
    }

    public void delete(Long id) {
        behaviourRepository.delete(findEntityById(id));
    }

    private void ensureUnique(BehaviourRequest request, Long excludingId) {
        behaviourRepository.findByStudent_Id(request.studentId())
                .filter(existing -> isDifferentRecord(existing, excludingId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "A behaviour record already exists for student id " + request.studentId());
                });
    }

    private boolean isDifferentRecord(Behaviour existing, Long excludingId) {
        return excludingId == null || !existing.getId().equals(excludingId);
    }

    private Behaviour findEntityById(Long id) {
        return behaviourRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Behaviour record not found with id: " + id));
    }

    private Student findStudentById(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentId));
    }

    private void applyRequest(Behaviour behaviour, BehaviourRequest request, Student student) {
        behaviour.setAttention(request.attention());
        behaviour.setParticipation(request.participation());
        behaviour.setDiscipline(request.discipline());
        behaviour.setHomeworkResponsibility(request.homeworkResponsibility());
        behaviour.setCommunicationSkills(request.communicationSkills());
        behaviour.setConfidence(request.confidence());
        behaviour.setTeamwork(request.teamwork());
        behaviour.setCuriosity(request.curiosity());
        behaviour.setLeadership(request.leadership());
        behaviour.setCriticalThinking(request.criticalThinking());
        behaviour.setOverallBehaviour(request.overallBehaviour());
        behaviour.setAnecdotalObservation(request.anecdotalObservation());
        behaviour.setStrength(request.strength());
        behaviour.setNeedsImprovement(request.needsImprovement());
        behaviour.setParentSupportRequired(request.parentSupportRequired());
        behaviour.setStudent(student);
    }

    private BehaviourResponse toResponse(Behaviour behaviour) {
        return new BehaviourResponse(
                behaviour.getId(),
                behaviour.getAttention(),
                behaviour.getParticipation(),
                behaviour.getDiscipline(),
                behaviour.getHomeworkResponsibility(),
                behaviour.getCommunicationSkills(),
                behaviour.getConfidence(),
                behaviour.getTeamwork(),
                behaviour.getCuriosity(),
                behaviour.getLeadership(),
                behaviour.getCriticalThinking(),
                behaviour.getOverallBehaviour(),
                behaviour.getAnecdotalObservation(),
                behaviour.getStrength(),
                behaviour.getNeedsImprovement(),
                behaviour.getParentSupportRequired(),
                behaviour.getStudent().getId(),
                behaviour.getCreatedAt(),
                behaviour.getUpdatedAt()
        );
    }
}

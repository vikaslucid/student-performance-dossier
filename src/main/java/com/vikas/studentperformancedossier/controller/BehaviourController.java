package com.vikas.studentperformancedossier.controller;

import com.vikas.studentperformancedossier.dto.BehaviourRequest;
import com.vikas.studentperformancedossier.dto.BehaviourResponse;
import com.vikas.studentperformancedossier.service.BehaviourService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/behaviours")
public class BehaviourController {

    private final BehaviourService behaviourService;

    public BehaviourController(BehaviourService behaviourService) {
        this.behaviourService = behaviourService;
    }

    @GetMapping
    public List<BehaviourResponse> getAllBehaviours() {
        return behaviourService.findAll();
    }

    @GetMapping("/{id}")
    public BehaviourResponse getBehaviourById(@PathVariable Long id) {
        return behaviourService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public BehaviourResponse createBehaviour(@Valid @RequestBody BehaviourRequest request) {
        return behaviourService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
    public BehaviourResponse updateBehaviour(@PathVariable Long id, @Valid @RequestBody BehaviourRequest request) {
        return behaviourService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteBehaviour(@PathVariable Long id) {
        behaviourService.delete(id);
    }
}

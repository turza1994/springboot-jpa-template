package com.example.app.project.controller;

import com.example.app.project.dto.CreateProjectRequest;
import com.example.app.project.dto.ProjectResponse;
import com.example.app.project.dto.UpdateProjectRequest;
import com.example.app.project.service.ProjectService;
import com.example.app.common.response.ApiResponse;
import com.example.app.common.response.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(
            @Valid @RequestBody CreateProjectRequest request,
            HttpServletRequest httpRequest) {

        ProjectResponse response = projectService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response, (String) httpRequest.getAttribute("requestId")));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        ProjectResponse response = projectService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response, (String) httpRequest.getAttribute("requestId")));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            HttpServletRequest httpRequest) {

        PageResponse<ProjectResponse> response = PageResponse.of(projectService.getAll(pageable));
        return ResponseEntity.ok(ApiResponse.success(response, (String) httpRequest.getAttribute("requestId")));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProjectRequest request,
            HttpServletRequest httpRequest) {

        ProjectResponse response = projectService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, (String) httpRequest.getAttribute("requestId")));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {

        projectService.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null, (String) httpRequest.getAttribute("requestId")));
    }

}
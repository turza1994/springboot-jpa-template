package com.example.app.project.mapper;

import com.example.app.project.dto.CreateProjectRequest;
import com.example.app.project.dto.ProjectResponse;
import com.example.app.project.dto.UpdateProjectRequest;
import com.example.app.project.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(CreateProjectRequest request) {
        if (request == null) {
            return null;
        }
        return Project.builder()
            .name(request.getName())
            .description(request.getDescription())
            .build();
    }

    public void updateEntity(Project project, UpdateProjectRequest request) {
        if (project == null || request == null) {
            return;
        }
        project.setName(request.getName());
        project.setDescription(request.getDescription());
    }

    public ProjectResponse toResponse(Project project) {
        return ProjectResponse.from(project);
    }

}
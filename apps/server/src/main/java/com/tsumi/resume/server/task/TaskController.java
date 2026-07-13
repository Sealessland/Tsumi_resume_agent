package com.tsumi.resume.server.task;

import com.tsumi.resume.workflow.CreateTaskCommand;
import com.tsumi.resume.workflow.TaskOrchestrator;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

    private final TaskOrchestrator orchestrator;

    public TaskController(TaskOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping
    ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request) {
        var task = orchestrator.create(new CreateTaskCommand(
                request.resumeId(), request.baseVersion(), request.jobDescription()));
        var location = URI.create("/api/v1/tasks/" + task.taskId());
        return ResponseEntity.created(location).body(TaskResponse.from(task));
    }

    @GetMapping("/{taskId}")
    TaskResponse get(@PathVariable("taskId") String taskId) {
        return TaskResponse.from(orchestrator.get(taskId));
    }
}

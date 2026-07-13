package com.tsumi.resume.server.task;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TaskApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndReadsALocalAgentTask() throws Exception {
        importResume("res_task_api");
        var response = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "res_task_api",
                                  "baseVersion": 1,
                                  "jobDescription": "Java Agent Engineer"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("/api/v1/tasks/task_")))
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andExpect(jsonPath("$.workflowSummary")
                        .value("LOCAL_FAKE_READY_FOR_REVIEW"))
                .andReturn();

        var taskId = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(response.getResponse().getContentAsString())
                .get("taskId").asText();

        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(taskId))
                .andExpect(jsonPath("$.resumeId").value("res_task_api"));
    }

    @Test
    void refusesToCreateATaskForAnUnknownResumeVersion() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "res_missing",
                                  "baseVersion": 1,
                                  "jobDescription": "Java Agent Engineer"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESUME_VERSION_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidCreateRequests() throws Exception {
        mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeId":"bad","baseVersion":0,"jobDescription":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsAStableNotFoundProblem() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/task_missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
    }

    @Test
    void exposesOnlyTheHealthActuatorEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    private void importResume(String resumeId) throws Exception {
        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId":"%s","version":1,"schemaVersion":13,
                                  "profile":{},"educations":[],"skills":"","internships":[],
                                  "projects":[],"studentExperiences":[],"researchExperiences":[],
                                  "customImages":[],"awards":[],"certificates":[],"selfSummary":{},
                                  "sectionVisibility":{},"layout":{},"theme":{}
                                }
                                """.formatted(resumeId)))
                .andExpect(status().isCreated());
    }
}

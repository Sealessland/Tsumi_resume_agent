package com.tsumi.resume.server.review;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ResumeReviewApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void importsReviewsAndAtomicallyMergesANewResumeVersion() throws Exception {
        var resume = (ObjectNode) objectMapper.readTree(Path.of(
                System.getProperty("contracts.dir"),
                "fixtures/resume/valid-minimal-v13.json").toFile());
        resume.put("resumeId", "res_api_flow");

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(resume)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/resumes/res_api_flow/versions/1"))
                .andExpect(jsonPath("$.resumeId").value("res_api_flow"))
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(resume)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));

        var taskResponse = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId": "res_api_flow",
                                  "baseVersion": 1,
                                  "jobDescription": "Java Agent Engineer"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("REVIEW_REQUIRED"))
                .andReturn();
        var taskId = objectMapper.readTree(taskResponse.getResponse().getContentAsByteArray())
                .path("taskId").asText();

        var proposalRequest = objectMapper.createObjectNode();
        var proposal = (ObjectNode) objectMapper.readTree(Path.of(
                System.getProperty("contracts.dir"),
                "fixtures/patch/valid-paraphrase-proposal.json").toFile());
        proposal.put("patchId", "rp_api_flow");
        proposal.put("taskId", taskId);
        proposal.put("resumeId", "res_api_flow");
        proposalRequest.set("proposal", proposal);
        proposalRequest.set("assessment", objectMapper.readTree("""
                {
                  "evidenceCoverage": 1.0,
                  "newAtomicClaims": [],
                  "riskFlags": []
                }
                """));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/patch-proposals", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(proposalRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/tasks/" + taskId + "/patches/rp_api_flow"))
                .andExpect(jsonPath("$.policyDecision").value("ALLOW"))
                .andExpect(jsonPath("$.reviewStatus").value("PENDING"))
                .andExpect(jsonPath("$.newAtomicClaims").isEmpty());

        mockMvc.perform(post(
                                "/api/v1/tasks/{taskId}/patches/{patchId}/decision",
                                taskId,
                                "rp_api_flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedBaseVersion":2,"decision":"ACCEPTED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VERSION_CONFLICT"));

        mockMvc.perform(post(
                                "/api/v1/tasks/{taskId}/patches/{patchId}/decision",
                                taskId,
                                "rp_api_flow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedBaseVersion":1,"decision":"ACCEPTED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewStatus").value("ACCEPTED"));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/merge", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedBaseVersion\":1}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/resumes/res_api_flow/versions/2"))
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.projects[0].description")
                        .value("打通结构化编辑、实时预览及 PDF/PNG 导出链路。"));

        mockMvc.perform(get("/api/v1/resumes/res_api_flow/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumeId").value("res_api_flow"))
                .andExpect(jsonPath("$.versions", contains(1, 2)));

        mockMvc.perform(get("/api/v1/resumes/res_api_flow/versions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));

        mockMvc.perform(get("/api/v1/tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void rejectsUnsupportedClaimsWithoutPersistingAPatch() throws Exception {
        var resume = (ObjectNode) objectMapper.readTree(Path.of(
                System.getProperty("contracts.dir"),
                "fixtures/resume/valid-minimal-v13.json").toFile());
        resume.put("resumeId", "res_policy_api");
        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(resume)))
                .andExpect(status().isCreated());

        var taskResponse = mockMvc.perform(post("/api/v1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "resumeId":"res_policy_api",
                                  "baseVersion":1,
                                  "jobDescription":"Java Agent Engineer"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        var taskId = objectMapper.readTree(taskResponse.getResponse().getContentAsByteArray())
                .path("taskId").asText();

        var proposal = (ObjectNode) objectMapper.readTree(Path.of(
                System.getProperty("contracts.dir"),
                "fixtures/patch/valid-paraphrase-proposal.json").toFile());
        proposal.put("patchId", "rp_unsupported_metric");
        proposal.put("taskId", taskId);
        proposal.put("resumeId", "res_policy_api");
        var request = objectMapper.createObjectNode();
        request.set("proposal", proposal);
        request.set("assessment", objectMapper.readTree("""
                {
                  "evidenceCoverage":1.0,
                  "newAtomicClaims":["导出耗时降低 50%"],
                  "riskFlags":["UNSUPPORTED_METRIC"]
                }
                """));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/patch-proposals", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("POLICY_REJECTED"))
                .andExpect(jsonPath("$.violations[0]")
                        .value("UNSUPPORTED_ATOMIC_CLAIM"));

        mockMvc.perform(get("/api/v1/tasks/{taskId}/patches", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/api/v1/resumes/res_policy_api/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versions", contains(1)));
    }

    @Test
    void rejectsPayloadsThatDoNotMatchThePublishedSchemas() throws Exception {
        mockMvc.perform(post("/api/v1/resumes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeId\":\"res_invalid\",\"schemaVersion\":13}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTRACT_REJECTED"))
                .andExpect(jsonPath("$.contract").value("resume"))
                .andExpect(jsonPath("$.violations").isNotEmpty());

        var invalidProposal = objectMapper.createObjectNode();
        var proposal = (ObjectNode) objectMapper.readTree(Path.of(
                System.getProperty("contracts.dir"),
                "fixtures/patch/valid-paraphrase-proposal.json").toFile());
        proposal.putArray("evidenceRefs");
        invalidProposal.set("proposal", proposal);
        invalidProposal.set("assessment", objectMapper.readTree("""
                {"evidenceCoverage":1.0,"newAtomicClaims":[],"riskFlags":[]}
                """));

        mockMvc.perform(post("/api/v1/tasks/task_fixture/patch-proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(invalidProposal)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CONTRACT_REJECTED"))
                .andExpect(jsonPath("$.contract").value("resume-patch-proposal"));
    }
}

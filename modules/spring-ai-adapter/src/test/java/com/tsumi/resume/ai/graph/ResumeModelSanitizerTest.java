package com.tsumi.resume.ai.graph;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ResumeModelSanitizerTest {

    @Test
    void removesDirectAndNestedPiiBeforeAnyModelCall() throws Exception {
        var resume = (com.fasterxml.jackson.databind.node.ObjectNode) new ObjectMapper().readTree("""
                {
                  "schemaVersion": 13,
                  "resumeId": "resume_01",
                  "version": 1,
                  "basics": {
                    "name": "张三",
                    "email": "zhangsan@example.com",
                    "phone": "13800000000",
                    "photo": "https://example.com/avatar.jpg",
                    "address": "杭州市西湖区",
                    "summary": "Java 工程师"
                  },
                  "profiles": [{"platform": "GitHub", "url": "https://github.com/example"}]
                }
                """);

        var view = new ResumeModelSanitizer().sanitize(resume);
        var json = view.content().toString();

        assertThat(json).doesNotContain("张三", "zhangsan@example.com", "13800000000", "杭州市西湖区", "avatar.jpg");
        assertThat(json).contains("Java 工程师", "GitHub");
        assertThat(view.resumeId()).isEqualTo("resume_01");
        assertThat(view.version()).isEqualTo(1);
    }
}

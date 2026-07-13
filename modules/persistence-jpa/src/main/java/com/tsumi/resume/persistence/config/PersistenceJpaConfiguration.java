package com.tsumi.resume.persistence.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.persistence.task.JpaTaskRepositoryAdapter;
import com.tsumi.resume.persistence.task.TaskJpaRepository;
import com.tsumi.resume.task.TaskRepository;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration(proxyBeanMethods = false)
@EntityScan("com.tsumi.resume.persistence")
@EnableJpaRepositories("com.tsumi.resume.persistence")
@ComponentScan("com.tsumi.resume.persistence")
public class PersistenceJpaConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObjectMapper persistenceObjectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    TaskRepository taskRepository(TaskJpaRepository repository) {
        return new JpaTaskRepositoryAdapter(repository);
    }
}

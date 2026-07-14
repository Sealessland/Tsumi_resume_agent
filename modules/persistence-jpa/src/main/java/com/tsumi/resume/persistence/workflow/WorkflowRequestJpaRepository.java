package com.tsumi.resume.persistence.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRequestJpaRepository extends JpaRepository<WorkflowRequestEntity, String> {}

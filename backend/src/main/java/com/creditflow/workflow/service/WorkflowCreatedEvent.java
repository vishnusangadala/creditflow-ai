package com.creditflow.workflow.service;

import java.util.UUID;

/**
 * Published when a workflow has been created and its documents stored. A
 * {@code @TransactionalEventListener(AFTER_COMMIT)} starts processing only once
 * the creating transaction has committed, so the row is visible to the async
 * worker thread.
 */
public record WorkflowCreatedEvent(UUID workflowId) {}

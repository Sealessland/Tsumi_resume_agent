package com.tsumi.resume.ai.graph;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategyFactoryBuilder;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.tsumi.resume.task.WorkflowNode;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.workflow.ResumeAgentWorkflow;
import com.tsumi.resume.workflow.WorkflowInput;
import com.tsumi.resume.workflow.WorkflowResult;
import com.tsumi.resume.workflow.WorkflowExecutionException;
import com.tsumi.resume.workflow.WorkflowObserver;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import com.tsumi.resume.workflow.review.CoverageGap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A deterministic Spring AI Alibaba Graph. Agents can produce structured data, but
 * cannot choose the workflow route or the final evidence policy decision.
 */
public final class SpringAiAlibabaResumeWorkflow implements ResumeAgentWorkflow {

    static final String HUMAN_REVIEW = "human_review";
    private static final String LOAD_RESUME = "load_resume";
    private static final String JD_ANALYST = "jd_analyst";
    private static final String REWRITE_AGENT = "rewrite_agent";
    private static final String PRECHECK = "deterministic_precheck";
    private static final String EVIDENCE_GUARD = "evidence_guard";
    private static final String REPAIR_AGENT = "repair_agent";
    private static final String AGGREGATE = "aggregate_review";

    private static final String INPUT = "workflowInput";
    private static final String MODEL_VIEW = "resumeModelView";
    private static final String MATRIX = "capabilityMatrix";
    private static final String PROPOSALS = "proposals";
    private static final String PRECHECK_RESULT = "precheckResult";
    private static final String ACCEPTED = "acceptedProposals";
    private static final String GAPS = "coverageGaps";
    private static final String REPAIR_COUNT = "repairCount";
    private static final String SUMMARY = "summary";
    private static final String OBSERVER = "workflowObserver";

    private final ResumeVersionReader resumes;
    private final ResumeModelSanitizer sanitizer;
    private final StructuredJdAnalyst analyst;
    private final StructuredResumeRewriter rewriter;
    private final ProposalPreChecker preChecker;
    private final WorkflowEvidenceVerifier verifier;
    private final VerifiedProposalSink sink;
    private final CompiledGraph graph;

    public SpringAiAlibabaResumeWorkflow(
            ResumeVersionReader resumes,
            ResumeModelSanitizer sanitizer,
            StructuredJdAnalyst analyst,
            StructuredResumeRewriter rewriter,
            ProposalPreChecker preChecker,
            WorkflowEvidenceVerifier verifier,
            VerifiedProposalSink sink) {
        this.resumes = resumes;
        this.sanitizer = sanitizer;
        this.analyst = analyst;
        this.rewriter = rewriter;
        this.preChecker = preChecker;
        this.verifier = verifier;
        this.sink = sink;
        this.graph = compileGraph();
    }

    @Override
    public WorkflowResult execute(WorkflowInput input) {
        return execute(input, WorkflowObserver.noop());
    }

    @Override
    public WorkflowResult execute(WorkflowInput input, WorkflowObserver observer) {
        var config = RunnableConfig.builder()
                .threadId(input.taskId())
                .addMetadata(OBSERVER, observer)
                .build();
        graph.stream(Map.of(
                        INPUT, input,
                        ACCEPTED, List.of(),
                        GAPS, List.of(),
                        REPAIR_COUNT, input.repairCount()), config)
                .blockLast();
        var snapshot = graph.getState(config);
        var summary = snapshot.state().value(SUMMARY, String.class)
                .orElseThrow(() -> new IllegalStateException("Graph stopped without a review summary"));
        return new WorkflowResult(summary);
    }

    public StateSnapshot snapshot(String taskId) {
        return graph.getState(RunnableConfig.builder().threadId(taskId).build());
    }

    private CompiledGraph compileGraph() {
        var strategies = new KeyStrategyFactoryBuilder().defaultStrategy(new ReplaceStrategy());
        List.of(INPUT, MODEL_VIEW, MATRIX, PROPOSALS, PRECHECK_RESULT, ACCEPTED, GAPS, REPAIR_COUNT, SUMMARY)
                .forEach(key -> strategies.addStrategy(key, new ReplaceStrategy()));
        var saver = MemorySaver.builder().build();
        try {
            return new StateGraph("tsumi-resume-review", strategies.build())
                    .addNode(LOAD_RESUME, observed(WorkflowNode.LOAD_RESUME, this::loadResume))
                    .addNode(JD_ANALYST, observed(WorkflowNode.JD_ANALYST, this::analyzeJd))
                    .addNode(REWRITE_AGENT, observed(WorkflowNode.REWRITE_AGENT, state -> rewrite(state, false)))
                    .addNode(PRECHECK, observed(WorkflowNode.DETERMINISTIC_PRECHECK, this::precheck))
                    .addNode(EVIDENCE_GUARD, observed(WorkflowNode.EVIDENCE_GUARD, this::verifyEvidence))
                    .addNode(REPAIR_AGENT, observed(WorkflowNode.REPAIR_AGENT, state -> rewrite(state, true)))
                    .addNode(AGGREGATE, observed(WorkflowNode.REVIEW_AGGREGATOR, this::aggregate))
                    .addNode(HUMAN_REVIEW, AsyncNodeActionWithConfig.node_async((state, config) -> Map.of()))
                    .addEdge(START, LOAD_RESUME)
                    .addEdge(LOAD_RESUME, JD_ANALYST)
                    .addEdge(JD_ANALYST, REWRITE_AGENT)
                    .addEdge(REWRITE_AGENT, PRECHECK)
                    .addEdge(PRECHECK, EVIDENCE_GUARD)
                    .addConditionalEdges(EVIDENCE_GUARD, edge_async(this::routeAfterEvidence), Map.of(
                            "repair", REPAIR_AGENT,
                            "review", AGGREGATE))
                    .addEdge(REPAIR_AGENT, PRECHECK)
                    .addEdge(AGGREGATE, HUMAN_REVIEW)
                    .addEdge(HUMAN_REVIEW, END)
                    .compile(CompileConfig.builder()
                            .saverConfig(SaverConfig.builder().register(saver).build())
                            .interruptBefore(HUMAN_REVIEW)
                            .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot compile fixed resume workflow graph", exception);
        }
    }

    private AsyncNodeActionWithConfig observed(WorkflowNode node, NodeAction action) {
        return AsyncNodeActionWithConfig.node_async((state, config) -> {
            var observer = config.metadata(OBSERVER)
                    .filter(WorkflowObserver.class::isInstance)
                    .map(WorkflowObserver.class::cast)
                    .orElseGet(WorkflowObserver::noop);
            var started = System.nanoTime();
            observer.nodeStarted(node);
            try {
                var result = action.apply(state);
                observer.nodeCompleted(node, elapsedMillis(started));
                return result;
            } catch (Exception exception) {
                var code = exception instanceof WorkflowExecutionException controlled
                        ? controlled.code() : "NODE_EXECUTION_FAILED";
                observer.nodeFailed(node, code, elapsedMillis(started));
                throw exception;
            }
        });
    }

    private long elapsedMillis(long startedNanos) {
        return Math.max(0, java.util.concurrent.TimeUnit.NANOSECONDS
                .toMillis(System.nanoTime() - startedNanos));
    }

    private Map<String, Object> loadResume(OverAllState state) {
        var input = required(state, INPUT, WorkflowInput.class);
        return Map.of(MODEL_VIEW, sanitizer.sanitize(resumes.get(input.resumeId(), input.baseVersion())));
    }

    private Map<String, Object> analyzeJd(OverAllState state) {
        var input = required(state, INPUT, WorkflowInput.class);
        return Map.of(MATRIX, analyst.analyze(input.jobDescription()));
    }

    private Map<String, Object> rewrite(OverAllState state, boolean repair) {
        var request = new RewriteRequest(
                required(state, INPUT, WorkflowInput.class),
                required(state, MODEL_VIEW, ResumeModelView.class),
                required(state, MATRIX, CapabilityMatrix.class),
                list(state, GAPS, CoverageGap.class),
                repair);
        var update = new LinkedHashMap<String, Object>();
        update.put(PROPOSALS, List.copyOf(rewriter.propose(request)));
        if (repair) update.put(REPAIR_COUNT, required(state, REPAIR_COUNT, Integer.class) + 1);
        return update;
    }

    private Map<String, Object> precheck(OverAllState state) {
        var result = preChecker.check(
                required(state, INPUT, WorkflowInput.class),
                required(state, MODEL_VIEW, ResumeModelView.class),
                list(state, PROPOSALS, PatchProposal.class));
        return Map.of(PRECHECK_RESULT, result);
    }

    private Map<String, Object> verifyEvidence(OverAllState state) {
        var input = required(state, INPUT, WorkflowInput.class);
        var checked = required(state, PRECHECK_RESULT, PrecheckResult.class);
        var verified = checked.candidates().isEmpty()
                ? new VerificationResult(List.of(), List.of())
                : verifier.verify(input, checked.candidates());
        var allGaps = new ArrayList<CoverageGap>(checked.gaps());
        allGaps.addAll(verified.gaps());
        return Map.of(
                ACCEPTED, mergeByPatchId(list(state, ACCEPTED, PatchProposal.class), verified.supported()),
                GAPS, List.copyOf(allGaps));
    }

    private String routeAfterEvidence(OverAllState state) {
        var gaps = list(state, GAPS, CoverageGap.class);
        var repairCount = required(state, REPAIR_COUNT, Integer.class);
        return !gaps.isEmpty() && repairCount == 0 ? "repair" : "review";
    }

    private Map<String, Object> aggregate(OverAllState state) {
        var input = required(state, INPUT, WorkflowInput.class);
        var accepted = list(state, ACCEPTED, PatchProposal.class);
        var gaps = list(state, GAPS, CoverageGap.class);
        var repairs = required(state, REPAIR_COUNT, Integer.class);
        sink.submit(input.taskId(), accepted, gaps);
        return Map.of(SUMMARY, "REVIEW_READY; proposals=%d; gaps=%d; repairs=%d"
                .formatted(accepted.size(), gaps.size(), repairs));
    }

    private List<PatchProposal> mergeByPatchId(List<PatchProposal> previous, List<PatchProposal> current) {
        var merged = new LinkedHashMap<String, PatchProposal>();
        previous.forEach(proposal -> merged.put(proposal.patchId(), proposal));
        current.forEach(proposal -> merged.put(proposal.patchId(), proposal));
        return List.copyOf(merged.values());
    }

    private <T> T required(OverAllState state, String key, Class<T> type) {
        return state.value(key, type)
                .orElseThrow(() -> new IllegalStateException("Missing graph state: " + key));
    }

    private <T> List<T> list(OverAllState state, String key, Class<T> elementType) {
        var raw = state.value(key, List.class).orElse(List.of());
        var result = new ArrayList<T>();
        for (var item : raw) result.add(elementType.cast(item));
        return List.copyOf(result);
    }
}

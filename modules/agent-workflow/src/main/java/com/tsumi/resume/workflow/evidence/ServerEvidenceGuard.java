package com.tsumi.resume.workflow.evidence;

import com.fasterxml.jackson.databind.JsonNode;
import com.tsumi.resume.domain.evidence.ClaimAssessment;
import com.tsumi.resume.domain.evidence.ClaimVerdict;
import com.tsumi.resume.domain.evidence.EvidenceArtifact;
import com.tsumi.resume.domain.merge.ResumePathResolver;
import com.tsumi.resume.domain.policy.PatchAssessment;
import com.tsumi.resume.domain.policy.PatchProposal;
import com.tsumi.resume.task.ResumeTask;
import com.tsumi.resume.workflow.resume.ResumeVersionReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public final class ServerEvidenceGuard implements EvidenceGuard {

    private static final Pattern PROTECTED_FACT = Pattern.compile(
            "(?<![\\p{L}\\p{N}])(?:[$¥￥€£]\\s*)?\\d+(?:[.,]\\d+)?\\s*(?:%|USD|CNY|RMB|美元|万元|元|年|月|日)?"
                    + "|\\b(?:19|20)\\d{2}(?:[-/.]\\d{1,2}(?:[-/.]\\d{1,2})?)?\\b"
                    + "|\\b(?:[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+|[A-Z]{2,}|[A-Z][a-z]+[A-Z][A-Za-z]*)\\b");

    private final ResumeVersionReader resumes;
    private final ClaimSupportEvaluator evaluator;
    private final Clock clock;
    private final ResumePathResolver pathResolver = new ResumePathResolver();

    public ServerEvidenceGuard(
            ResumeVersionReader resumes,
            ClaimSupportEvaluator evaluator,
            Clock clock) {
        this.resumes = resumes;
        this.evaluator = evaluator;
        this.clock = clock;
    }

    @Override
    public PatchAssessment assess(
            ResumeTask task,
            PatchProposal proposal,
            List<EvidenceArtifact> approvedEvidence) {
        requireTarget(task, proposal);
        var evidence = resolveEvidence(task, proposal, approvedEvidence);
        requireBeforeMatches(task, proposal);

        var claims = new ArrayList<ClaimAssessment>();
        claims.addAll(deterministicProtectedFactChecks(proposal, evidence));
        claims.addAll(sanitizeModelVerdicts(evaluator.evaluate(
                proposal.before(), proposal.after(), evidence), evidence));
        if (claims.isEmpty()) {
            claims.add(new ClaimAssessment(
                    proposal.after(), ClaimVerdict.AMBIGUOUS, List.of(),
                    "No claim-level support verdict was produced"));
        }

        var uniqueClaims = deduplicate(claims);
        var supported = uniqueClaims.stream()
                .filter(claim -> claim.verdict() == ClaimVerdict.SUPPORTED)
                .count();
        var coverage = (double) supported / uniqueClaims.size();
        var unsupported = uniqueClaims.stream()
                .filter(claim -> claim.verdict() != ClaimVerdict.SUPPORTED)
                .map(ClaimAssessment::claim)
                .distinct()
                .toList();
        var riskFlags = uniqueClaims.stream()
                .filter(claim -> claim.verdict() != ClaimVerdict.SUPPORTED)
                .map(claim -> claim.verdict() == ClaimVerdict.AMBIGUOUS
                        ? "AMBIGUOUS_CLAIM" : "UNSUPPORTED_PROTECTED_FACT")
                .distinct()
                .toList();
        return new PatchAssessment(coverage, unsupported, riskFlags, uniqueClaims);
    }

    private List<EvidenceArtifact> resolveEvidence(
            ResumeTask task,
            PatchProposal proposal,
            List<EvidenceArtifact> candidates) {
        Map<String, EvidenceArtifact> byId = new LinkedHashMap<>();
        for (var artifact : candidates) byId.put(artifact.artifactId(), artifact);

        var resolved = new ArrayList<EvidenceArtifact>();
        for (var evidenceRef : proposal.evidenceRefs()) {
            var artifact = byId.get(evidenceRef);
            if (artifact == null
                    || !artifact.taskId().equals(task.taskId())
                    || !artifact.resumeId().equals(task.resumeId())
                    || !artifact.isUsableAt(clock.instant())) {
                throw new EvidenceNotApprovedException(evidenceRef);
            }
            resolved.add(artifact);
        }
        return List.copyOf(resolved);
    }

    private void requireTarget(ResumeTask task, PatchProposal proposal) {
        if (!proposal.taskId().equals(task.taskId())
                || !proposal.resumeId().equals(task.resumeId())
                || proposal.baseVersion() != task.baseVersion()) {
            throw new IllegalArgumentException("Proposal target does not match task");
        }
    }

    private void requireBeforeMatches(ResumeTask task, PatchProposal proposal) {
        JsonNode node = pathResolver.resolve(
                resumes.get(task.resumeId(), task.baseVersion()), proposal.path());
        var actual = node.isTextual() ? node.textValue() : node.toString();
        if (node.isMissingNode() || !actual.equals(proposal.before())) {
            throw new ProposalBaseMismatchException(proposal.path());
        }
    }

    private List<ClaimAssessment> deterministicProtectedFactChecks(
            PatchProposal proposal,
            List<EvidenceArtifact> evidence) {
        var source = new StringBuilder(proposal.before());
        evidence.forEach(artifact -> source.append('\n').append(artifact.excerpt()));
        var normalizedSource = source.toString().toLowerCase(Locale.ROOT);
        var claims = new ArrayList<ClaimAssessment>();
        var matcher = PROTECTED_FACT.matcher(proposal.after());
        while (matcher.find()) {
            var token = matcher.group().trim();
            if (!token.isBlank() && !normalizedSource.contains(token.toLowerCase(Locale.ROOT))) {
                claims.add(new ClaimAssessment(
                        token, ClaimVerdict.UNSUPPORTED, List.of(),
                        "Protected fact is absent from before and approved Evidence"));
            }
        }
        return claims;
    }

    private List<ClaimAssessment> sanitizeModelVerdicts(
            List<ClaimAssessment> modelClaims,
            List<EvidenceArtifact> evidence) {
        var allowedRefs = evidence.stream().map(EvidenceArtifact::artifactId).collect(java.util.stream.Collectors.toSet());
        return modelClaims.stream().map(claim -> {
            if (claim.evidenceRefs().stream().allMatch(allowedRefs::contains)) return claim;
            return new ClaimAssessment(
                    claim.claim(), ClaimVerdict.AMBIGUOUS, List.of(),
                    "Claim evaluator cited Evidence outside the approved task scope");
        }).toList();
    }

    private List<ClaimAssessment> deduplicate(List<ClaimAssessment> claims) {
        var unique = new LinkedHashMap<String, ClaimAssessment>();
        for (var claim : claims) {
            unique.putIfAbsent(claim.claim() + "\u0000" + claim.verdict(), claim);
        }
        return List.copyOf(unique.values());
    }
}

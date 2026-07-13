# M0A Shared Contracts Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring the approved upstream Vue application into this branch and establish versioned Resume and ResumePatch contracts that both Vue and Java validate against identical fixtures.

**Architecture:** Keep the existing Vue behavior intact while moving it under `apps/web`. Add a JDK 21 Maven reactor with a focused `resume-domain` module. Store protocol-neutral JSON Schemas and fixtures in root `contracts/`; frontend Vitest tests and Java JUnit tests must consume those same files so contract drift fails the build.

**Tech Stack:** Vue 3.5, Vite 8, Vitest, Ajv 8, JDK 21, Maven 3.9+, Jackson 2.18, NetworkNT JSON Schema Validator 1.5, JUnit 5, AssertJ

---

## Scope and Plan Sequence

The approved design spans multiple independently testable systems. Implement it as this plan sequence:

1. **M0A — Shared contracts foundation:** this document.
2. **M0B — Evidence policy, patch merge/revert, version conflicts, and task state machine.**
3. **M1 — PDF/DOCX + JD vertical slice, Spring AI Alibaba workflow, A2UI review, and human merge.**
4. **M2 — RocketMQ LiteTopic, external document worker, sandbox, SSE resume, traces, and dead-letter recovery.**
5. **M3 — A2A GitHub Evidence Agent and evidence approval.**
6. **M4 — evaluation harness, threat-model verification, judge demo, and open-source packaging.**

M0A is complete only when the imported Vue application still builds and both runtimes accept and reject the same contract fixtures.

## File Responsibility Map

### Existing upstream files moved without behavior changes

- `apps/web/src/**` — current Vue components, composables, and resume modules.
- `apps/web/public/**` — current static assets.
- `apps/web/package.json` — frontend dependencies and scripts.
- `apps/web/vite.config.js` — frontend Vite configuration.
- `apps/web/postcss.config.mjs` — frontend PostCSS configuration.
- `apps/web/index.html` — frontend entry document.

### New shared contract files

- `contracts/resume.schema.json` — structural Resume AST v13 envelope and stable entity identifiers.
- `contracts/resume-patch.schema.json` — evidence-first ResumePatch contract.
- `contracts/fixtures/resume/valid-minimal-v13.json` — cross-runtime valid Resume fixture.
- `contracts/fixtures/resume/invalid-missing-version.json` — cross-runtime invalid Resume fixture.
- `contracts/fixtures/patch/valid-paraphrase.json` — valid evidence-backed Patch fixture.
- `contracts/fixtures/patch/invalid-new-fact.json` — invalid unsupported-claim Patch fixture.

### New frontend contract files

- `apps/web/src/modules/resume/contracts.js` — v12-to-v13 envelope adapter and Ajv validators with deterministic result shape.
- `apps/web/src/modules/resume/contracts.test.js` — shared-fixture contract tests.
- `apps/web/vitest.config.js` — Vitest configuration.

### New Java files

- `pom.xml` — root Maven reactor and version management.
- `modules/resume-domain/pom.xml` — domain module dependencies and test configuration.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/ContractValidationResult.java` — immutable validation result.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/JsonContractValidator.java` — schema loading and validation.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/PatchIntent.java` — allowlisted patch intents.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/PatchOperation.java` — allowlisted replace and remove operations.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/PolicyDecision.java` — policy result values.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/ReviewStatus.java` — human review states.
- `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/ResumePatch.java` — Java Patch record matching the JSON contract.
- `modules/resume-domain/src/test/java/com/tsumi/resume/domain/contract/JsonContractValidatorTest.java` — shared-fixture Java tests.
- `modules/resume-domain/src/test/java/com/tsumi/resume/domain/patch/ResumePatchDeserializationTest.java` — enum and fixture deserialization tests.

### Deployment path adjustments

- `Dockerfile` — build the frontend from `apps/web`.

## Task 1: Import Upstream History and Preserve the Approved Design

**Files:**

- Preserve: `.gitignore`
- Preserve: `docs/superpowers/specs/2026-07-13-ai-agent-workbench-design.md`
- Import: reviewed upstream commit `596cf29`

- [ ] **Step 1: Verify the branch and clean worktree**

Run:

```bash
git branch --show-current
git status --short
```

Expected:

```text
codex/ai-agent-workbench-design
```

The status output must be empty before importing upstream.

- [ ] **Step 2: Add and fetch the source repository**

Run:

```bash
git remote add upstream https://github.com/kakerusan/tsumi_resume.git
git fetch upstream main
git cat-file -t 596cf29
```

Expected: the final command prints `commit`.

- [ ] **Step 3: Merge upstream without discarding the design root commit**

Run:

```bash
git merge --allow-unrelated-histories --no-ff --no-commit 596cf29
```

Expected: the merge stops with an add/add conflict only in `.gitignore`; upstream application files are present and the design specification remains present.

- [ ] **Step 4: Resolve the known `.gitignore` conflict and finish the merge**

Replace `.gitignore` with:

```gitignore
.superpowers/

# Logs
logs
*.log
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*
lerna-debug.log*

node_modules
dist
dist-ssr
*.local

# Editor directories and files
.vscode/*
!.vscode/extensions.json
.idea
.DS_Store
*.suo
*.ntvs*
*.njsproj
*.sln
*.sw?
```

Run:

```bash
git add .gitignore
git commit -m "chore: import tsumi resume upstream"
```

Expected: the merge commit succeeds with both parent histories preserved.

- [ ] **Step 5: Verify the imported frontend baseline**

Run:

```bash
npm ci
npm run build
```

Expected: Vite exits with code 0 and writes `dist/`.

- [ ] **Step 6: Commit only if merge hooks added unstaged normalization changes**

Run:

```bash
git status --short
```

Expected: empty. If a repository hook changed tracked line endings, inspect those changes and commit them separately as `chore: normalize imported frontend files`; do not combine behavior changes into this task.

## Task 2: Move the Frontend into `apps/web` Without Behavior Changes

**Files:**

- Move: `src/` → `apps/web/src/`
- Move: `public/` → `apps/web/public/`
- Move: `index.html` → `apps/web/index.html`
- Move: `package.json` → `apps/web/package.json`
- Move: `package-lock.json` → `apps/web/package-lock.json`
- Move: `vite.config.js` → `apps/web/vite.config.js`
- Move: `postcss.config.mjs` → `apps/web/postcss.config.mjs`
- Modify: `.gitignore`
- Modify: `Dockerfile`

- [ ] **Step 1: Move only frontend-owned files**

Run:

```bash
mkdir -p apps/web
git mv src apps/web/src
git mv public apps/web/public
git mv index.html apps/web/index.html
git mv package.json apps/web/package.json
git mv package-lock.json apps/web/package-lock.json
git mv vite.config.js apps/web/vite.config.js
git mv postcss.config.mjs apps/web/postcss.config.mjs
```

Expected: all commands succeed. Root deployment files and documentation remain at root.

- [ ] **Step 2: Update root ignore rules**

Replace `.gitignore` with:

```gitignore
.superpowers/

# Logs
logs
*.log
npm-debug.log*
yarn-debug.log*
yarn-error.log*
pnpm-debug.log*
lerna-debug.log*

node_modules
dist
dist-ssr
*.local
**/target/

# Editor directories and files
.vscode/*
!.vscode/extensions.json
.idea
.DS_Store
*.suo
*.ntvs*
*.njsproj
*.sln
*.sw?
*.iml
```

- [ ] **Step 3: Update the frontend Docker build context paths**

Replace `Dockerfile` with:

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY apps/web/package.json apps/web/package-lock.json ./
RUN npm ci
COPY apps/web/ ./
RUN npm run build

FROM nginx:1.27-alpine
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
```

- [ ] **Step 4: Install and build from the new location**

Run:

```bash
cd apps/web
npm ci
npm run build
```

Expected: Vite exits with code 0 and writes `apps/web/dist/`.

- [ ] **Step 5: Verify Docker path validity**

Run from repository root:

```bash
docker build -t tsumi-resume-web-buildcheck .
```

Expected: the Node build stage completes successfully.

- [ ] **Step 6: Commit the mechanical move**

```bash
git add .gitignore apps/web Dockerfile
git commit -m "chore: move frontend into apps workspace"
```

## Task 3: Establish the JDK 21 Maven Reactor

**Files:**

- Create: `pom.xml`
- Create: `modules/resume-domain/pom.xml`
- Create: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/DomainModuleSmokeTest.java`

- [ ] **Step 1: Write the failing domain smoke test**

Create `modules/resume-domain/src/test/java/com/tsumi/resume/domain/DomainModuleSmokeTest.java`:

```java
package com.tsumi.resume.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DomainModuleSmokeTest {

    @Test
    void runsOnJava21() {
        assertThat(Runtime.version().feature()).isGreaterThanOrEqualTo(21);
    }
}
```

- [ ] **Step 2: Run Maven to verify the reactor does not exist yet**

Run:

```bash
mvn -q -pl modules/resume-domain test
```

Expected: FAIL because the root and module POM files do not exist.

- [ ] **Step 3: Create the root Maven reactor**

Create `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.tsumi.resume</groupId>
  <artifactId>tsumi-resume-parent</artifactId>
  <version>0.1.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <modules>
    <module>modules/resume-domain</module>
  </modules>

  <properties>
    <maven.compiler.release>21</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <junit.version>5.11.4</junit.version>
    <assertj.version>3.27.3</assertj.version>
    <jackson.version>2.18.2</jackson.version>
    <json-schema-validator.version>1.5.6</json-schema-validator.version>
    <maven-surefire-plugin.version>3.5.2</maven-surefire-plugin.version>
  </properties>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.junit</groupId>
        <artifactId>junit-bom</artifactId>
        <version>${junit.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <dependency>
        <groupId>com.fasterxml.jackson</groupId>
        <artifactId>jackson-bom</artifactId>
        <version>${jackson.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>${maven-surefire-plugin.version}</version>
        </plugin>
      </plugins>
    </pluginManagement>
  </build>
</project>
```

- [ ] **Step 4: Create the domain module POM**

Create `modules/resume-domain/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>com.tsumi.resume</groupId>
    <artifactId>tsumi-resume-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
    <relativePath>../../pom.xml</relativePath>
  </parent>

  <artifactId>resume-domain</artifactId>

  <dependencies>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
      <groupId>com.networknt</groupId>
      <artifactId>json-schema-validator</artifactId>
      <version>${json-schema-validator.version}</version>
    </dependency>
    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.assertj</groupId>
      <artifactId>assertj-core</artifactId>
      <version>${assertj.version}</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <configuration>
          <systemPropertyVariables>
            <contracts.dir>${maven.multiModuleProjectDirectory}/contracts</contracts.dir>
          </systemPropertyVariables>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 5: Run the smoke test**

Run:

```bash
mvn -q -pl modules/resume-domain test
```

Expected: PASS with one test and zero failures.

- [ ] **Step 6: Commit the Java foundation**

```bash
git add pom.xml modules/resume-domain
git commit -m "build: establish Java domain module"
```

## Task 4: Add Shared Resume AST v13 Schema and Fixtures

**Files:**

- Create: `contracts/resume.schema.json`
- Create: `contracts/fixtures/resume/valid-minimal-v13.json`
- Create: `contracts/fixtures/resume/invalid-missing-version.json`

- [ ] **Step 1: Create the Resume AST schema**

Create `contracts/resume.schema.json`:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://tsumi.dev/contracts/resume.schema.json",
  "title": "Tsumi Resume AST v13",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "resumeId", "version", "schemaVersion", "profile", "educations",
    "skills", "internships", "projects", "studentExperiences",
    "researchExperiences", "customImages", "awards", "certificates", "selfSummary",
    "sectionVisibility", "layout", "theme"
  ],
  "properties": {
    "resumeId": { "type": "string", "pattern": "^res_[A-Za-z0-9_-]+$" },
    "version": { "type": "integer", "minimum": 1 },
    "schemaVersion": { "const": 13 },
    "profile": { "type": "object" },
    "educations": { "$ref": "#/$defs/entities" },
    "skills": { "type": "string" },
    "internships": { "$ref": "#/$defs/entities" },
    "projects": { "$ref": "#/$defs/entities" },
    "studentExperiences": { "$ref": "#/$defs/entities" },
    "researchExperiences": { "$ref": "#/$defs/entities" },
    "customImages": { "$ref": "#/$defs/entities" },
    "awards": { "$ref": "#/$defs/entities" },
    "certificates": { "$ref": "#/$defs/entities" },
    "selfSummary": { "type": "object" },
    "sectionVisibility": { "type": "object" },
    "layout": { "type": "object" },
    "theme": { "type": "object" }
  },
  "$defs": {
    "entity": {
      "type": "object",
      "required": ["id"],
      "properties": {
        "id": { "type": "string", "minLength": 1 }
      },
      "additionalProperties": true
    },
    "entities": {
      "type": "array",
      "items": { "$ref": "#/$defs/entity" }
    }
  }
}
```

This M0A schema freezes the envelope and stable entity IDs. Field-level schemas are added only when each section is migrated, preventing an unreviewed schema from silently changing existing frontend behavior.

- [ ] **Step 2: Create the valid fixture**

Create `contracts/fixtures/resume/valid-minimal-v13.json`:

```json
{
  "resumeId": "res_fixture",
  "version": 1,
  "schemaVersion": 13,
  "profile": {},
  "educations": [{ "id": "edu_01" }],
  "skills": "",
  "internships": [],
  "projects": [{ "id": "project_01" }],
  "studentExperiences": [],
  "researchExperiences": [],
  "customImages": [],
  "awards": [],
  "certificates": [],
  "selfSummary": {},
  "sectionVisibility": {},
  "layout": {},
  "theme": {}
}
```

- [ ] **Step 3: Create the invalid fixture**

Create `contracts/fixtures/resume/invalid-missing-version.json`:

```json
{
  "resumeId": "res_fixture",
  "schemaVersion": 13,
  "profile": {},
  "educations": [],
  "skills": "",
  "internships": [],
  "projects": [],
  "studentExperiences": [],
  "researchExperiences": [],
  "customImages": [],
  "awards": [],
  "certificates": [],
  "selfSummary": {},
  "sectionVisibility": {},
  "layout": {},
  "theme": {}
}
```

- [ ] **Step 4: Validate fixture syntax**

Run:

```bash
python3 -m json.tool contracts/resume.schema.json >/dev/null
python3 -m json.tool contracts/fixtures/resume/valid-minimal-v13.json >/dev/null
python3 -m json.tool contracts/fixtures/resume/invalid-missing-version.json >/dev/null
```

Expected: all commands exit with code 0 and print nothing.

- [ ] **Step 5: Commit the Resume contract**

```bash
git add contracts/resume.schema.json contracts/fixtures/resume
git commit -m "feat: define resume AST v13 contract"
```

## Task 5: Validate the Resume Contract in Vue

**Files:**

- Modify: `apps/web/package.json`
- Modify: `apps/web/package-lock.json`
- Create: `apps/web/vitest.config.js`
- Create: `apps/web/src/modules/resume/contracts.js`
- Create: `apps/web/src/modules/resume/contracts.test.js`

- [ ] **Step 1: Install frontend contract-test dependencies**

Run:

```bash
cd apps/web
npm install --save-dev vitest@3.2.4 ajv@8.17.1
npm pkg set scripts.test="vitest run"
```

Expected: `package.json` contains a `test` script and `package-lock.json` records Vitest and Ajv.

- [ ] **Step 2: Write the failing shared-fixture test**

Create `apps/web/src/modules/resume/contracts.test.js`:

```javascript
import { describe, expect, it } from 'vitest'
import validResume from '../../../../../contracts/fixtures/resume/valid-minimal-v13.json'
import invalidResume from '../../../../../contracts/fixtures/resume/invalid-missing-version.json'
import { createResumeEnvelope, validateResume } from './contracts'
import { createEmptyResume } from './templates'

describe('validateResume', () => {
  it('accepts the shared valid v13 fixture', () => {
    expect(validateResume(validResume)).toEqual({ valid: true, errors: [] })
  })

  it('rejects a fixture without version', () => {
    const result = validateResume(invalidResume)

    expect(result.valid).toBe(false)
    expect(result.errors).toContainEqual(expect.objectContaining({ keyword: 'required' }))
  })

  it('wraps the current local schema v12 data in a valid v13 server envelope', () => {
    const envelope = createResumeEnvelope(createEmptyResume(), {
      resumeId: 'res_fixture',
      version: 1,
    })

    expect(envelope.schemaVersion).toBe(13)
    expect(envelope).not.toHaveProperty('meta')
    expect(validateResume(envelope)).toEqual({ valid: true, errors: [] })
  })
})
```

- [ ] **Step 3: Run the test to verify it fails**

Run:

```bash
cd apps/web
npm test -- src/modules/resume/contracts.test.js
```

Expected: FAIL because `./contracts` does not exist.

- [ ] **Step 4: Configure Vitest**

Create `apps/web/vitest.config.js`:

```javascript
import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    environment: 'node',
    include: ['src/**/*.test.js'],
  },
})
```

- [ ] **Step 5: Implement the Ajv validator**

Create `apps/web/src/modules/resume/contracts.js`:

```javascript
import Ajv2020 from 'ajv/dist/2020'
import resumeSchema from '../../../../../contracts/resume.schema.json'
import { normalizeResumeData } from './normalize'

const ajv = new Ajv2020({ allErrors: true, strict: true })
const resumeValidator = ajv.compile(resumeSchema)

function normalizeErrors(errors = []) {
  return errors.map((error) => ({
    instancePath: error.instancePath,
    schemaPath: error.schemaPath,
    keyword: error.keyword,
    message: error.message || 'Contract validation failed',
  }))
}

export function validateResume(value) {
  const valid = resumeValidator(value)
  return {
    valid,
    errors: valid ? [] : normalizeErrors(resumeValidator.errors),
  }
}

export function createResumeEnvelope(source, { resumeId, version }) {
  const normalized = normalizeResumeData(source)
  const { meta: _localMeta, ...content } = normalized

  return {
    resumeId,
    version,
    schemaVersion: 13,
    ...content,
  }
}
```

- [ ] **Step 6: Run tests and build**

Run:

```bash
cd apps/web
npm test -- src/modules/resume/contracts.test.js
npm run build
```

Expected: three Vitest tests pass and Vite exits with code 0.

- [ ] **Step 7: Commit frontend contract validation**

```bash
git add apps/web/package.json apps/web/package-lock.json apps/web/vitest.config.js apps/web/src/modules/resume/contracts.js apps/web/src/modules/resume/contracts.test.js
git commit -m "test: validate resume contract in frontend"
```

## Task 6: Validate the Same Resume Fixtures in Java

**Files:**

- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/ContractValidationResult.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/JsonContractValidator.java`
- Create: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/contract/JsonContractValidatorTest.java`

- [ ] **Step 1: Write the failing Java contract tests**

Create `modules/resume-domain/src/test/java/com/tsumi/resume/domain/contract/JsonContractValidatorTest.java`:

```java
package com.tsumi.resume.domain.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class JsonContractValidatorTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));
    private final JsonContractValidator validator =
            new JsonContractValidator(contracts.resolve("resume.schema.json"));

    @Test
    void acceptsSharedValidResumeFixture() {
        var result = validator.validate(
                contracts.resolve("fixtures/resume/valid-minimal-v13.json"));

        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void rejectsSharedFixtureWithoutVersion() {
        var result = validator.validate(
                contracts.resolve("fixtures/resume/invalid-missing-version.json"));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(message -> message.contains("version"));
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -q -pl modules/resume-domain -Dtest=JsonContractValidatorTest test
```

Expected: FAIL because `JsonContractValidator` and `ContractValidationResult` do not exist.

- [ ] **Step 3: Create the immutable validation result**

Create `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/ContractValidationResult.java`:

```java
package com.tsumi.resume.domain.contract;

import java.util.List;

public record ContractValidationResult(boolean valid, List<String> errors) {

    public ContractValidationResult {
        errors = List.copyOf(errors);
    }
}
```

- [ ] **Step 4: Implement the schema validator**

Create `modules/resume-domain/src/main/java/com/tsumi/resume/domain/contract/JsonContractValidator.java`:

```java
package com.tsumi.resume.domain.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.List;

public final class JsonContractValidator {

    private final ObjectMapper objectMapper;
    private final JsonSchema schema;

    public JsonContractValidator(Path schemaPath) {
        this.objectMapper = new ObjectMapper();
        this.schema = JsonSchemaFactory
                .getInstance(SpecVersion.VersionFlag.V202012)
                .getSchema(read(schemaPath));
    }

    public ContractValidationResult validate(Path documentPath) {
        var messages = schema.validate(read(documentPath));
        List<String> errors = messages.stream()
                .map(Object::toString)
                .sorted()
                .toList();
        return new ContractValidationResult(errors.isEmpty(), errors);
    }

    private JsonNode read(Path path) {
        try {
            return objectMapper.readTree(path.toFile());
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot read JSON contract artifact: " + path, exception);
        }
    }
}
```

- [ ] **Step 5: Run the Java tests**

Run:

```bash
mvn -q -pl modules/resume-domain -Dtest=JsonContractValidatorTest test
```

Expected: two tests pass and zero fail.

- [ ] **Step 6: Commit Java contract validation**

```bash
git add modules/resume-domain/src/main modules/resume-domain/src/test/java/com/tsumi/resume/domain/contract
git commit -m "test: validate resume contract in Java"
```

## Task 7: Define the Evidence-first ResumePatch Contract

**Files:**

- Create: `contracts/resume-patch.schema.json`
- Create: `contracts/fixtures/patch/valid-paraphrase.json`
- Create: `contracts/fixtures/patch/invalid-new-fact.json`
- Modify: `apps/web/src/modules/resume/contracts.js`
- Modify: `apps/web/src/modules/resume/contracts.test.js`

- [ ] **Step 1: Create the Patch JSON Schema**

Create `contracts/resume-patch.schema.json`:

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://tsumi.dev/contracts/resume-patch.schema.json",
  "title": "Tsumi ResumePatch",
  "type": "object",
  "additionalProperties": false,
  "required": [
    "patchId", "taskId", "resumeId", "baseVersion", "op", "path",
    "before", "after", "intent", "evidenceRefs", "jdRefs",
    "evidenceCoverage", "newAtomicClaims", "confidence", "riskFlags",
    "policyDecision", "reviewStatus"
  ],
  "properties": {
    "patchId": { "type": "string", "pattern": "^rp_[A-Za-z0-9_-]+$" },
    "taskId": { "type": "string", "pattern": "^task_[A-Za-z0-9_-]+$" },
    "resumeId": { "type": "string", "pattern": "^res_[A-Za-z0-9_-]+$" },
    "baseVersion": { "type": "integer", "minimum": 1 },
    "op": { "enum": ["replace", "remove"] },
    "path": { "type": "string", "pattern": "^/" },
    "before": { "type": "string" },
    "after": { "type": "string" },
    "intent": {
      "enum": ["PARAPHRASE", "RESTRUCTURE", "COMPRESS", "DELETE", "EXTRACT_SUPPORTED_KEYWORD"]
    },
    "evidenceRefs": {
      "type": "array",
      "minItems": 1,
      "uniqueItems": true,
      "items": { "type": "string", "minLength": 1 }
    },
    "jdRefs": {
      "type": "array",
      "uniqueItems": true,
      "items": { "type": "string", "minLength": 1 }
    },
    "evidenceCoverage": { "const": 1.0 },
    "newAtomicClaims": { "type": "array", "maxItems": 0 },
    "confidence": { "type": "number", "minimum": 0, "maximum": 1 },
    "riskFlags": {
      "type": "array",
      "uniqueItems": true,
      "items": { "type": "string", "minLength": 1 }
    },
    "policyDecision": { "const": "ALLOW" },
    "reviewStatus": { "enum": ["PENDING", "ACCEPTED", "REJECTED", "EDITED"] }
  },
  "allOf": [
    {
      "if": { "properties": { "op": { "const": "remove" } }, "required": ["op"] },
      "then": { "properties": { "intent": { "const": "DELETE" }, "after": { "const": "" } } }
    }
  ]
}
```

- [ ] **Step 2: Create the valid Patch fixture**

Create `contracts/fixtures/patch/valid-paraphrase.json`:

```json
{
  "patchId": "rp_fixture",
  "taskId": "task_fixture",
  "resumeId": "res_fixture",
  "baseVersion": 1,
  "op": "replace",
  "path": "/projects/project_01/highlights/highlight_01",
  "before": "实现简历编辑和导出功能。",
  "after": "打通结构化编辑、实时预览及 PDF/PNG 导出链路。",
  "intent": "PARAPHRASE",
  "evidenceRefs": ["resume:projects/project_01"],
  "jdRefs": ["jd:delivery/export"],
  "evidenceCoverage": 1.0,
  "newAtomicClaims": [],
  "confidence": 0.92,
  "riskFlags": [],
  "policyDecision": "ALLOW",
  "reviewStatus": "PENDING"
}
```

- [ ] **Step 3: Create the invalid unsupported-claim fixture**

Create `contracts/fixtures/patch/invalid-new-fact.json`:

```json
{
  "patchId": "rp_invalid",
  "taskId": "task_fixture",
  "resumeId": "res_fixture",
  "baseVersion": 1,
  "op": "replace",
  "path": "/projects/project_01/highlights/highlight_01",
  "before": "实现简历导出功能。",
  "after": "将简历导出耗时降低 50%。",
  "intent": "PARAPHRASE",
  "evidenceRefs": ["resume:projects/project_01"],
  "jdRefs": [],
  "evidenceCoverage": 0.5,
  "newAtomicClaims": ["导出耗时降低 50%"],
  "confidence": 0.99,
  "riskFlags": ["UNSUPPORTED_METRIC"],
  "policyDecision": "REJECT",
  "reviewStatus": "PENDING"
}
```

- [ ] **Step 4: Extend the frontend test before the implementation**

Append to `apps/web/src/modules/resume/contracts.test.js`:

```javascript
import validPatch from '../../../../../contracts/fixtures/patch/valid-paraphrase.json'
import invalidPatch from '../../../../../contracts/fixtures/patch/invalid-new-fact.json'

describe('validateResumePatch', () => {
  it('accepts an evidence-backed paraphrase', () => {
    expect(validateResumePatch(validPatch)).toEqual({ valid: true, errors: [] })
  })

  it('rejects a patch containing a new unsupported fact', () => {
    expect(validateResumePatch(invalidPatch).valid).toBe(false)
  })
})
```

Also change the existing import to:

```javascript
import { createResumeEnvelope, validateResume, validateResumePatch } from './contracts'
```

- [ ] **Step 5: Run the frontend test to verify it fails**

Run:

```bash
cd apps/web
npm test -- src/modules/resume/contracts.test.js
```

Expected: FAIL because `validateResumePatch` is not exported.

- [ ] **Step 6: Implement Patch validation in the frontend**

Add to `apps/web/src/modules/resume/contracts.js`:

```javascript
import resumePatchSchema from '../../../../../contracts/resume-patch.schema.json'

const resumePatchValidator = ajv.compile(resumePatchSchema)

export function validateResumePatch(value) {
  const valid = resumePatchValidator(value)
  return {
    valid,
    errors: valid ? [] : normalizeErrors(resumePatchValidator.errors),
  }
}
```

- [ ] **Step 7: Run frontend contract tests**

Run:

```bash
cd apps/web
npm test -- src/modules/resume/contracts.test.js
```

Expected: five tests pass and zero fail.

- [ ] **Step 8: Commit Patch schemas and frontend validation**

```bash
git add contracts/resume-patch.schema.json contracts/fixtures/patch apps/web/src/modules/resume/contracts.js apps/web/src/modules/resume/contracts.test.js
git commit -m "feat: define evidence-first resume patch contract"
```

## Task 8: Bind ResumePatch to Java Types and Shared Fixtures

**Files:**

- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/PatchIntent.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/PatchOperation.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/PolicyDecision.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/ReviewStatus.java`
- Create: `modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch/ResumePatch.java`
- Create: `modules/resume-domain/src/test/java/com/tsumi/resume/domain/patch/ResumePatchDeserializationTest.java`

- [ ] **Step 1: Write the failing deserialization and contract tests**

Create `modules/resume-domain/src/test/java/com/tsumi/resume/domain/patch/ResumePatchDeserializationTest.java`:

```java
package com.tsumi.resume.domain.patch;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tsumi.resume.domain.contract.JsonContractValidator;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ResumePatchDeserializationTest {

    private final Path contracts = Path.of(System.getProperty("contracts.dir"));
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesSharedParaphraseFixture() throws Exception {
        ResumePatch patch = objectMapper.readValue(
                contracts.resolve("fixtures/patch/valid-paraphrase.json").toFile(),
                ResumePatch.class);

        assertThat(patch.intent()).isEqualTo(PatchIntent.PARAPHRASE);
        assertThat(patch.evidenceRefs()).containsExactly("resume:projects/project_01");
        assertThat(patch.newAtomicClaims()).isEmpty();
    }

    @Test
    void schemaRejectsSharedUnsupportedClaimFixture() {
        var validator = new JsonContractValidator(
                contracts.resolve("resume-patch.schema.json"));

        assertThat(validator.validate(
                contracts.resolve("fixtures/patch/invalid-new-fact.json")).valid()).isFalse();
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```bash
mvn -q -pl modules/resume-domain -Dtest=ResumePatchDeserializationTest test
```

Expected: FAIL because the Patch Java types do not exist.

- [ ] **Step 3: Create the enum types**

Create `PatchIntent.java`:

```java
package com.tsumi.resume.domain.patch;

public enum PatchIntent {
    PARAPHRASE,
    RESTRUCTURE,
    COMPRESS,
    DELETE,
    EXTRACT_SUPPORTED_KEYWORD
}
```

Create `PatchOperation.java`:

```java
package com.tsumi.resume.domain.patch;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PatchOperation {
    @JsonProperty("replace") REPLACE,
    @JsonProperty("remove") REMOVE
}
```

Create `PolicyDecision.java`:

```java
package com.tsumi.resume.domain.patch;

public enum PolicyDecision {
    ALLOW,
    REJECT
}
```

Create `ReviewStatus.java`:

```java
package com.tsumi.resume.domain.patch;

public enum ReviewStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    EDITED
}
```

- [ ] **Step 4: Create the ResumePatch record**

Create `ResumePatch.java`:

```java
package com.tsumi.resume.domain.patch;

import java.util.List;

public record ResumePatch(
        String patchId,
        String taskId,
        String resumeId,
        long baseVersion,
        PatchOperation op,
        String path,
        String before,
        String after,
        PatchIntent intent,
        List<String> evidenceRefs,
        List<String> jdRefs,
        double evidenceCoverage,
        List<String> newAtomicClaims,
        double confidence,
        List<String> riskFlags,
        PolicyDecision policyDecision,
        ReviewStatus reviewStatus) {

    public ResumePatch {
        evidenceRefs = List.copyOf(evidenceRefs);
        jdRefs = List.copyOf(jdRefs);
        newAtomicClaims = List.copyOf(newAtomicClaims);
        riskFlags = List.copyOf(riskFlags);
    }
}
```

- [ ] **Step 5: Run all Java domain tests**

Run:

```bash
mvn -q -pl modules/resume-domain test
```

Expected: all tests pass and zero fail.

- [ ] **Step 6: Commit Java Patch types**

```bash
git add modules/resume-domain/src/main/java/com/tsumi/resume/domain/patch modules/resume-domain/src/test/java/com/tsumi/resume/domain/patch
git commit -m "feat: bind resume patch contract to Java domain"
```

## Task 9: Add One-command Contract Verification

**Files:**

- Create: `scripts/verify-contracts.sh`
- Modify: `README.md`

- [ ] **Step 1: Create the verification script**

Create `scripts/verify-contracts.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$repo_root/apps/web"
npm ci
npm test -- src/modules/resume/contracts.test.js
npm run build

cd "$repo_root"
mvn -q -pl modules/resume-domain test
```

Run:

```bash
chmod +x scripts/verify-contracts.sh
```

- [ ] **Step 2: Document the contract boundary**

Add this section to `README.md` after the technical stack section:

```markdown
## AI 全栈改造：共享契约基础

前端与 Java 后端共享根目录 `contracts/` 中的 Resume AST 和 ResumePatch JSON Schema。
任何协议修改必须同时通过 Vue/Vitest 与 Java/JUnit 的相同 fixture，避免两端数据模型漂移。

```bash
./scripts/verify-contracts.sh
```

ResumePatch 当前只允许有完整证据引用的改写、重组、压缩、删除和已支持关键词抽取。
无证据数字、新增事实和占位指标会在 Schema 层直接拒绝。
```

- [ ] **Step 3: Run the complete verification script**

Run:

```bash
./scripts/verify-contracts.sh
```

Expected:

- frontend contract tests: five pass, zero fail;
- Vite production build: exit code 0;
- Java domain tests: all pass, zero fail.

- [ ] **Step 4: Check repository hygiene**

Run:

```bash
git diff --check
git status --short
```

Expected: `git diff --check` prints nothing. Status lists only `README.md` and `scripts/verify-contracts.sh` before commit.

- [ ] **Step 5: Commit verification documentation**

```bash
git add README.md scripts/verify-contracts.sh
git commit -m "docs: add shared contract verification workflow"
```

## M0A Completion Gate

Run from repository root:

```bash
./scripts/verify-contracts.sh
git status --short
git log --oneline -8
```

Expected:

- the verification script exits with code 0;
- worktree status is empty;
- recent history shows small commits for upstream import, workspace move, Java foundation, Resume schema, frontend validation, Java validation, Patch contract, Java Patch types, and verification documentation.

Do not begin M0B until this gate passes. M0B must start from these exact Resume AST and ResumePatch contracts and add deterministic Policy Guard, stable-ID path resolution, patch apply/revert, optimistic version checks, and the task state machine.

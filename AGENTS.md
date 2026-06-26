# AGENTS.md

## Project Snapshot
- This is a Spring Boot 4 + Spring AI backend that runs a synchronous autonomous code-generation loop (`/executor-agent/execute`).
- Core orchestration lives in `src/main/java/com/example/springia/service/AgentExecutionService.java`.
- The loop is: validate request -> discovery -> plan prompt -> LLM JSON response -> file writes -> compile backend+frontend -> feedback-driven retry.
- Persistence is first-class: each run stores `Execution`, `Attempt`, `ArtifactChange`, `CompilationLog`, `ExecutionStatus` entities under `src/main/java/com/example/springia/entity/`.

## High-Value Code Map
- API entrypoint: `src/main/java/com/example/springia/controller/ExecutionController.java` (`POST /executor-agent/execute`).
- Execution orchestration loop: `src/main/java/com/example/springia/service/AgentExecutionService.java` (main agent loop with retry/feedback logic).
- LLM integration and tool-calling: `src/main/java/com/example/springia/agent/service/SpringAiPromptGenerationClient.java`.
- Tools (Spring AI `@Tool` beans): `src/main/java/com/example/springia/agent/tool/**` (discovery, files, compiler, feedback, diff).
- Advisors (plan/scope/repair/verification): `src/main/java/com/example/springia/agent/advisor/**`.
- Model/DTOs: `src/main/java/com/example/springia/agent/model/**` (GeneratedChangeSet, FileChangeCommand, CompilationResult, etc).
- Runtime settings (roots/timeouts/retries/model): `src/main/resources/application.properties` + `src/main/java/com/example/springia/config/AgentProperties.java`.
- Data persistence: `src/main/java/com/example/springia/entity/**` (Execution, Attempt, ArtifactChange, CompilationLog, ExecutionStatus).

## Commands Agents Should Use
- Run tests: `mvn clean test` (backend standard and used by compile tool for `compileBy=command`).
- Run app locally: `./mvnw spring-boot:run`.
- Start local PostgreSQL + pgvector: `docker compose up -d`.
- Compile strategy from request `compileBy`:
  - `COMMAND`: backend `mvn clean test`, frontend `ng build`.
  - `DOCKER`: requires `Dockerfile` in each target repo, then build/run via Docker.

## Code Contracts You Must Respect
- `ExecutionRequest` currently accepts `taskDescription` and `compileBy` only (`src/main/java/com/example/springia/dto/ExecutionRequest.java`).
- LLM response is parsed as `GeneratedChangeSet` JSON; parser tolerates fenced markdown and alias `files` for `changes` (`AgentExecutionServiceTest`).
- File writes must stay inside `agent.backend-root` or `agent.frontend-root`; out-of-scope paths throw immediately (`ScopeAdvisor`, `FileWriteTool`).
- Logging convention is strict in this codebase: `@Slf4j`, method-tag prefixes like `{[EXEC_SVC]}`, and actuator logger curl in class Javadoc (see `ExecutionController`, `CompilationTool`).
- **Tool Annotations**: All agent tools use `@Tool` and `@ToolParam` from `org.springframework.ai.tool.annotation` (Spring AI framework). Tools must be `@Component` beans; they are auto-wired into `SpringAiPromptGenerationClient` and registered via `.tools(bean1, bean2, ...)` in the ChatClient call.
- **Tool-Calling System Prompt**: Instructs the LLM to use tools when needed; the prompt is in `SpringAiPromptGenerationClient.TOOL_CALLING_SYSTEM_PROMPT` and enforces tool usage for discovery, validation, and compilation feedback.

## JPA/DB Conventions Already in Use
- Entities use Portuguese-prefixed columns (`nu_`, `co_`, `de_`, `dh_`, `ic_`) and explicit `@Column(name=...)`.
- Table naming pattern is active: `@Entity(name = "lictbNNN_nome")` + sequence `licsqNNN_*`.
- Lombok data model style is standard for entities: `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
- Long text is mapped with `@Lob` (see `Execution.deSolicitacao`, `CompilationLog.deSaida`).

## Implemented Tools (Spring AI @Tool Beans)
All tools are `@Component` beans located in `src/main/java/com/example/springia/agent/tool/**`:
- **ProjectDiscoveryTool** (`discovery/`): Maps backend/frontend structure, identifies Java files, controllers, entities, tests. Core entry point for understanding project state.
- **FileReadTool** (`files/`): Reads file contents from allowed roots (backend/frontend). Used to confirm current state before LLM generates changes.
- **FileWriteTool** (`files/`): Applies file changes with backup and scope validation. Prevents writes outside allowed directories.
- **CompilationTool** (`compiler/`): Executes `mvn clean test` (backend) or `ng build` (frontend) via command or Docker. Collects exit codes, stdout, stderr, and timeout status.
- **CodeDiffTool** (`diff/`): Summarizes line-count diffs between before/after file content (added, removed, total lines).
- **FeedbackTool** (`feedback/`): Consolidates compilation errors, test failures, and diff summaries into structured feedback for next LLM iteration.
- **ProcessExecutor** (`process/`): Underlying executor for running external processes with timeout and result capture (used by CompilationTool).

## Integration Points / External Dependencies
- OpenAI model access is via Spring AI (`spring.ai.openai.*` in `application.properties`, model set to `gpt-5.3-codex`).
- Azure Spring dependency is present in `pom.xml`, but main runtime path here is Spring AI + JPA + PostgreSQL.
- Tools are wired into `SpringAiPromptGenerationClient.generate()` via `.tools(projectDiscoveryTool, fileReadTool, compilationTool, feedbackTool, codeDiffTool)` (see line 66 in SpringAiPromptGenerationClient).

## Implemented Advisors
Located in `src/main/java/com/example/springia/agent/advisor/**`:
- **PlanningAdvisor**: Converts task description + discovery summary into initial execution plan. Sets the tone for LLM instructions (see `buildPlan()`).
- **ScopeAdvisor**: Validates that file changes stay within allowed backend/frontend roots. Throws immediately if out-of-scope.
- **RepairAdvisor**: Builds repair prompts from compilation feedback and previous LLM response when iteration fails. Chains feedback into next prompt.
- **VerificationAdvisor**: Checks if all compilation results are successful (no errors, exit code 0, no timeout). Gate for marking iteration as "VALIDADO".

## Agent Execution Loop & Feedback Mechanism
The main loop in `AgentExecutionService.execute()` (lines 101-170):
1. **Iteration N**: Generate prompt (PlanningAdvisor on first iteration, RepairAdvisor after failures).
2. **LLM Call**: Submit prompt + tools to LLM via ChatClient; LLM may invoke tools and return JSON `GeneratedChangeSet`.
3. **File Writes**: Parse `GeneratedChangeSet` and write changes via FileWriteTool (with scope validation).
4. **Compilation**: Run CompilationTool (backend + frontend) and capture results.
5. **Verification**: Check VerificationAdvisor; if all green, mark attempt "VALIDADO" and end loop with success.
6. **Feedback**: If any compilation fails, FeedbackTool consolidates errors and RepairAdvisor builds next prompt.
7. **Retry**: Loop up to `agent.max-iterations` times (default 100) or until success.

All iterations are persisted: Execution → Attempts → ArtifactChanges + CompilationLogs.

## Practical Warnings
- Everything in this codebase is in Portuguese (logs, messages, column names, advisors). Stay consistent with language.
- FileWriteTool creates backups in `java.io.tmpdir + "/springia-backups"` before writes; review backup strategy if required.
- CompilationTool supports both command-line and Docker modes; Docker requires each project to have a Dockerfile.
- Prefer current Java sources over legacy root markdown docs; some root docs mention older classes/fields (`projectId`, `AgentLoop`) not present in active API.
- Keep changes minimal and localized: this service is retry-heavy and stores full execution history; noisy refactors increase diff risk and feedback churn.
- When changing orchestration, update unit tests in `src/test/java/com/example/springia/service/AgentExecutionServiceTest.java` first; it documents accepted parsing and success criteria.


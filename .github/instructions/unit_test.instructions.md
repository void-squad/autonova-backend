---
applyTo: '**'
---
Provide project context and coding guidelines that AI should follow when generating code, answering questions, or reviewing changes.

# Test Automation Instructions

This document explains how an automated agent (or developer) should generate unit tests across services, run them, collect JaCoCo coverage, and open a PR. It includes an actionable TODO checklist the agent should follow.

---

## Environment prerequisites

- Java 17 (matching project), Maven (`mvn`) or use `./mvnw` wrapper
- Git CLI and access to push branches to the repository
- `gh` (GitHub CLI) or API token to open PRs (optional but recommended)
- Docker running on the runner if tests use Testcontainers

If running in CI (GitHub Actions), the provided workflows will run tests and collect coverage.

---

## High-level plan

1. Analyze each service for missing or sparse tests.
2. Generate JUnit5 + Mockito unit tests for service/map/controller layers.
3. Run tests per-module to validate locally; fix issues.
4. Run `mvn verify` to produce JaCoCo reports per module and aggregated at root.
5. Commit tests on a feature branch and open a PR against `dev` (or target branch).
6. Monitor CI and iterate until tests and coverage are reported.

---

## Commands (copy/paste)

- Create and switch to feature branch:
```bash
git checkout -b tests/add-unit-tests-all-services
```

- Run tests for a single module (fast during development):
```bash
mvn -pl progress-monitoring-service -am -DskipTests=false test
```

- Run tests and generate JaCoCo reports for a single module:
```bash
mvn -pl progress-monitoring-service -am -DskipTests=false verify
# Module reports:
# progress-monitoring-service/target/site/jacoco/jacoco.xml
# progress-monitoring-service/target/site/jacoco/index.html
```

- Run tests for all modules and aggregated coverage (root):
```bash
mvn -B -DskipTests=false verify
# Aggregated reports:
# target/site/jacoco-aggregate/jacoco.xml
# target/site/jacoco-aggregate/index.html
```

- Run a single test class:
```bash
mvn -pl progress-monitoring-service -am -Dtest=ProjectMessageServiceTest test
```

- Commit and push changes:
```bash
git add -A
git commit -m "tests: add unit tests for <service>"
git push -u origin tests/add-unit-tests-all-services
```

- Create a PR with GitHub CLI:
```bash
gh pr create --title "chore(tests): add unit tests and coverage" \
  --body "Adds unit tests for multiple services and generates JaCoCo reports. See CI for details." \
  --base dev
```

If `gh` is not available, open the PR using the GitHub web UI.

---

## Test generation guidance

- Use JUnit 5, Mockito, and AssertJ for unit tests.
- Prioritize writing tests for:
  - Service layer (mock repositories/clients)
  - Mappers (pure functions)
  - Controllers (`@WebMvcTest` + MockMvc)
  - Security logic (use `@WithMockUser` or test `SecurityFilterChain` separately)
- Avoid heavy integration tests in the unit-test suite. Put integration tests behind a profile or suffix them `*IT` and run separately.
- Mock external systems (DB, MQ, HTTP) in unit tests. Use Testcontainers only where real services are required.

### Test patterns

- Service tests: `@ExtendWith(MockitoExtension.class)`, `@InjectMocks` service, `@Mock` dependencies, use `ArgumentCaptor` to assert repository interactions.
- Mapper tests: plain unit tests asserting field-by-field mapping and null handling.
- Controller tests: `@WebMvcTest`, `@MockBean` for service beans, use `MockMvc` to perform requests and expect JSON and status.

Example reference: `progress-monitoring-service/src/test/java/.../ProjectMessageServiceTest.java` (added already).

---

## CI and coverage notes

- The repository includes a workflow `.github/workflows/test-and-coverage.yml` which:
  - Detects Java and .NET services, runs tests per-service, uploads per-service coverage artifacts, aggregates coverage, and comments a `COVERAGE.md` on PRs.
  - We added a Codecov upload step in the aggregate job that will publish coverage if the repo secret `CODECOV_TOKEN` is configured.
- Ensure each module produces `target/site/jacoco/jacoco.xml`. If not, add `jacoco-maven-plugin` to that module's `pom.xml` (some modules already had this added).

---

## Agent capabilities and constraints

- An automated/cloud agent can run unit tests and open PRs if it has:
  - Java/Maven and Git access, and optionally Docker for Testcontainers.
  - A GitHub token (`GITHUB_TOKEN` or personal access token) to push branches and open PRs or use `gh` CLI.
- If the agent cannot run Docker, it should still generate tests and push the branch; GitHub Actions will run CI where Docker is available if configured.

---

## PR checklist for the agent (must be included in PR description)

- Branch name: `tests/<service>` or `tests/all-services`.
- Tests added under `src/test/java` for each module.
- Module-level tests run locally: `mvn -pl <module> -am test` (attach local run results if available).
- `mvn verify` generates per-module `jacoco.xml` and root `jacoco-aggregate/jacoco.xml`.
- CI: link to `test-and-coverage` workflow run and attached artifacts.
- Short summary of tests added and coverage delta if available.

---

## Recommended phased rollout

1. Phase 1: progress-monitoring-service, appointment-booking-service, auth-service
2. Phase 2: customer-service, gateway-service, discovery-service
3. Phase 3: employee-dashboard-service, notification-service, payments-billing-service, time-logging-service, analytics-service, chatbot

Create a PR per phase to keep iteration small and CI feedback manageable.

---

## TODO checklist (for the agent)

- [ ] Analyze repository test gaps
  - Scan each service for missing or sparse unit tests (focus on services, controllers, mappers, repositories). Produce a per-service test coverage target list.
- [ ] Generate unit tests per service
  - Create JUnit5 + Mockito unit tests for service, mapper, and controller layers for each module. Use Testcontainers only for integration tests; prefer mocking for unit tests.
- [ ] Run tests and collect coverage locally
  - Run `mvn -B -DskipTests=false verify` per module and at root to generate JaCoCo reports; fix failing tests if any.
- [ ] Open feature branch and PR
  - Commit tests to a feature branch `tests/<service>-coverage` or `tests/all-services`, push, and open PR with coverage summary and CI link.
- [ ] Monitor CI and address failures
  - Iterate on failing tests or CI environment issues (secrets, Docker availability), re-run until CI passes and coverage is reported.

---

If you want, I can start Phase 1 by generating tests for the next service you choose — or run `mvn -pl progress-monitoring-service -am verify` here and attach the generated `jacoco.xml` and test output.

---

Last updated: 2025-11-19

# Testing & Code Coverage Guide

This guide explains how to run tests, generate test reports, and view code coverage for the Autonova Backend microservices.

## Table of Contents
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Test Types](#test-types)
- [Running Tests](#running-tests)
- [Test Reports](#test-reports)
- [Code Coverage](#code-coverage)
- [CI/CD Integration](#cicd-integration)
- [Troubleshooting](#troubleshooting)

## Overview

The testing infrastructure includes:
- **JUnit 5** for unit and integration tests
- **Mockito** for mocking dependencies
- **TestContainers** for integration tests with real databases/messaging
- **REST Assured** for API testing
- **JaCoCo** for code coverage reporting
- **Maven Surefire** for unit test execution
- **Maven Failsafe** for integration test execution

### Coverage Thresholds
- **Line Coverage**: 60% minimum
- **Branch Coverage**: 50% minimum

## Quick Start

### Run All Tests with Reports
```bash
# Make script executable (first time only)
chmod +x run-tests.sh

# Run all tests and generate reports
./run-tests.sh
```

### View Reports
After running tests, open these files in your browser:
- **Unit Tests**: `<service>/target/site/surefire-report.html`
- **Integration Tests**: `<service>/target/site/failsafe-report.html`
- **Code Coverage**: `<service>/target/site/jacoco/index.html`

## Test Types

### Unit Tests
- Files ending with `Test.java` or `Tests.java`
- Fast, isolated tests with mocked dependencies
- Run with: `mvn test`

### Integration Tests
- Files ending with `IT.java` or `IntegrationTest.java`
- Use TestContainers for real database/messaging
- Run with: `mvn integration-test`

## Running Tests

### Using the Script (Recommended)

```bash
# Run all tests
./run-tests.sh

# Run only unit tests
./run-tests.sh -t unit

# Run only integration tests
./run-tests.sh -t integration

# Run tests for specific services
./run-tests.sh -s customer-service,auth-service

# Skip coverage threshold checks (useful during development)
./run-tests.sh -c

# Run without generating HTML reports
./run-tests.sh -n
```

### Using Maven Directly

#### All Services

```bash
# Unit tests only
mvn clean test

# Integration tests only
mvn clean integration-test

# All tests with coverage
mvn clean verify

# Skip coverage checks
mvn clean verify -Djacoco.skip=true
```

#### Single Service

```bash
# Navigate to service directory
cd customer-service

# Run unit tests
mvn test

# Run all tests with coverage
mvn verify

# Generate HTML reports
mvn surefire-report:report site -DgenerateReports=false
```

## Test Reports

### Generated Reports

After running tests, the following reports are generated:

#### 1. Unit Test Reports (Surefire)
**Location**: `target/site/surefire-report.html`

**Contains**:
- Test execution summary
- Pass/fail status for each test
- Execution time
- Error messages and stack traces

**Generate manually**:
```bash
mvn surefire-report:report
```

#### 2. Integration Test Reports (Failsafe)
**Location**: `target/site/failsafe-report.html`

**Contains**:
- Integration test execution results
- TestContainers logs
- Database/messaging test results

**Generate manually**:
```bash
mvn failsafe-report:failsafe-report-only
```

#### 3. Code Coverage Reports (JaCoCo)
**Location**: `target/site/jacoco/index.html`

**Contains**:
- Line coverage percentage
- Branch coverage percentage
- Package-level breakdown
- Class-level details
- Missed/covered lines highlighted

**Additional JaCoCo Reports**:
- `target/site/jacoco-merged/index.html` - Combined unit + integration coverage
- `target/jacoco.exec` - Binary coverage data (unit tests)
- `target/jacoco-it.exec` - Binary coverage data (integration tests)
- `target/jacoco-merged.exec` - Merged binary data

#### 4. XML Reports (for CI/CD)
**Location**: 
- `target/surefire-reports/*.xml` - Unit test results
- `target/failsafe-reports/*.xml` - Integration test results

These XML files are used by CI/CD tools like Jenkins, GitHub Actions, GitLab CI.

### Viewing Reports

#### Open HTML Reports
```bash
# Customer service unit tests
open customer-service/target/site/surefire-report.html

# Customer service coverage
open customer-service/target/site/jacoco/index.html

# All services (opens in browser)
find . -name "surefire-report.html" -path "*/target/site/*" | xargs open
```

#### Generate Site Report (All Reports in One Place)
```bash
# Generate complete site with all reports
mvn site

# View at target/site/index.html
```

## Code Coverage

### Understanding Coverage Metrics

**JaCoCo measures**:
1. **Line Coverage**: Percentage of code lines executed
2. **Branch Coverage**: Percentage of decision branches taken
3. **Instruction Coverage**: Bytecode instruction coverage
4. **Complexity Coverage**: Cyclomatic complexity coverage
5. **Method Coverage**: Methods invoked vs total methods
6. **Class Coverage**: Classes instantiated vs total classes

### Coverage Thresholds

Configured in `pom.xml`:
```xml
<jacoco.line.coverage>0.60</jacoco.line.coverage>
<jacoco.branch.coverage>0.50</jacoco.branch.coverage>
```

Build **fails** if coverage is below threshold. Override with:
```bash
mvn verify -Djacoco.skip=true
```

### Improving Coverage

1. **Identify gaps**: Open `target/site/jacoco/index.html`
2. **Navigate to classes**: Click package → class
3. **View missed lines**: Red = not covered, Green = covered, Yellow = partial
4. **Write tests**: Focus on red/yellow lines
5. **Re-run**: `mvn clean verify`

### Coverage Reports for Multiple Services

```bash
# Generate coverage for all services
mvn clean verify

# Aggregate reports (view each service separately)
for service in */; do
  echo "Coverage for $service:"
  open "$service/target/site/jacoco/index.html"
done
```

## CI/CD Integration

### GitHub Actions Example

```yaml
name: Test and Coverage

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
          
      - name: Run Tests
        run: mvn clean verify
        
      - name: Generate Coverage Report
        run: mvn jacoco:report
        
      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./target/site/jacoco/jacoco.xml
          
      - name: Publish Test Report
        uses: mikepenz/action-junit-report@v3
        if: always()
        with:
          report_paths: '**/target/surefire-reports/TEST-*.xml'
          
      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results
          path: |
            **/target/surefire-reports/
            **/target/failsafe-reports/
            **/target/site/jacoco/
```

### GitLab CI Example

```yaml
test:
  stage: test
  script:
    - mvn clean verify
  artifacts:
    when: always
    reports:
      junit:
        - "**/target/surefire-reports/TEST-*.xml"
        - "**/target/failsafe-reports/TEST-*.xml"
      coverage_report:
        coverage_format: jacoco
        path: "**/target/site/jacoco/jacoco.xml"
    paths:
      - "**/target/site/jacoco/"
      - "**/target/surefire-reports/"
  coverage: '/Total.*?([0-9]{1,3})%/'
```

### Jenkins Pipeline Example

```groovy
pipeline {
    agent any
    stages {
        stage('Test') {
            steps {
                sh 'mvn clean verify'
            }
        }
        stage('Publish Reports') {
            steps {
                junit '**/target/surefire-reports/*.xml'
                jacoco(
                    execPattern: '**/target/jacoco.exec',
                    classPattern: '**/target/classes',
                    sourcePattern: '**/src/main/java'
                )
                publishHTML(target: [
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'target/site',
                    reportFiles: 'surefire-report.html',
                    reportName: 'Test Report'
                ])
            }
        }
    }
}
```

## Troubleshooting

### Common Issues

#### 1. Tests Fail Due to Coverage Threshold
```
[ERROR] Rule violated for bundle: Minimum line coverage is 0.60, but was 0.45
```

**Solution A**: Write more tests to increase coverage
**Solution B**: Temporarily skip check:
```bash
mvn verify -Djacoco.skip=true
```

#### 2. TestContainers Cannot Start
```
Could not find a valid Docker environment
```

**Solution**: Ensure Docker is running
```bash
# Check Docker
docker ps

# Start Docker (Mac/Linux)
systemctl start docker
```

#### 3. H2 Database Schema Issues
```
Caused by: org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement
```

**Solution**: Escape reserved keywords in entities:
```java
@Column(name = "`year`")  // Escape reserved keyword
private Integer year;
```

#### 4. RabbitMQ Connection Refused in Tests
```
com.rabbitmq.client.PossibleAuthenticationFailureException: Possibly caused by authentication failure
```

**Solution**: Tests should use `@TestConfiguration` with embedded RabbitMQ or mock:
```java
@TestConfiguration
static class TestConfig {
    @Bean
    public ConnectionFactory connectionFactory() {
        return mock(ConnectionFactory.class);
    }
}
```

#### 5. Out of Memory During Tests
```
java.lang.OutOfMemoryError: Java heap space
```

**Solution**: Already configured in parent POM:
```xml
<argLine>@{argLine} -Xmx1024m</argLine>
```

Increase if needed in specific service POM:
```xml
<argLine>@{argLine} -Xmx2048m</argLine>
```

#### 6. Reports Not Generated
```bash
# If reports are missing, generate manually
mvn surefire-report:report
mvn site -DgenerateReports=false
```

### Viewing Detailed Test Output

```bash
# Run with verbose output
mvn test -X

# Run specific test
mvn test -Dtest=CustomerServiceTest

# Run tests matching pattern
mvn test -Dtest=*ServiceTest
```

### Clean Build

If experiencing weird issues:
```bash
# Clean all build artifacts
mvn clean

# Clean and rebuild
mvn clean install

# Clean specific service
cd customer-service && mvn clean
```

## Best Practices

### Writing Testable Code
1. Use dependency injection
2. Avoid static methods
3. Keep methods small and focused
4. Use interfaces for dependencies

### Test Naming
```java
// Unit tests
CustomerServiceTest.java
VehicleServiceTest.java

// Integration tests
CustomerServiceIT.java
DatabaseIntegrationIT.java
```

### Test Organization
```java
@Test
@DisplayName("Should create customer when valid data provided")
void shouldCreateCustomer_WhenValidData() {
    // Given
    Customer customer = createValidCustomer();
    
    // When
    Customer result = customerService.create(customer);
    
    // Then
    assertThat(result.getId()).isNotNull();
}
```

### Mocking Best Practices
```java
// Mock dependencies
@Mock
private CustomerRepository repository;

@InjectMocks
private CustomerService service;

// Use BDD style
given(repository.findById(1L)).willReturn(Optional.of(customer));

// Verify interactions
verify(repository).save(any(Customer.class));
```

### TestContainers Usage
```java
@Testcontainers
class CustomerServiceIT {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");
    
    @Test
    void integrationTest() {
        // Test with real database
    }
}
```

## Additional Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [TestContainers Guide](https://www.testcontainers.org/)
- [JaCoCo Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [REST Assured](https://rest-assured.io/)

## Summary

```bash
# Quick Reference
./run-tests.sh                          # Run all tests
./run-tests.sh -t unit                  # Unit tests only
./run-tests.sh -s customer-service      # Specific service
mvn verify                              # All tests with coverage
mvn test                                # Unit tests only
open */target/site/jacoco/index.html    # View coverage
```

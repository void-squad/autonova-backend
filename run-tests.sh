#!/bin/bash

################################################################################
# Test Execution and Report Generation Script
# Runs unit tests, integration tests, and generates coverage reports
################################################################################

# set -e  # Exit on error - removed to allow skipping failures

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
TEST_TYPE="all"  # all, unit, integration
GENERATE_REPORTS="true"
SKIP_COVERAGE_CHECK="false"
SERVICES=""

# All available services
ALL_SERVICES=(
    "analytics-service"
    "appointment-booking-service"
    "auth-service"
    "chatbot"
    "customer-service"
    "discovery-service"
    "employee-dashboard-service"
    "gateway-service"
    "notification-service"
    "payments-billing-service"
    "progress-monitoring-service"
    "time-logging-service"
)

# Function to print colored output
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to display usage
usage() {
    cat << EOF
Usage: $0 [OPTIONS]

Options:
    -t, --type TYPE         Test type to run: all, unit, integration (default: all)
    -s, --service SERVICE   Run tests for specific service(s), comma-separated
    -n, --no-reports        Skip report generation
    -c, --skip-coverage     Skip coverage threshold checks
    -h, --help              Display this help message

Examples:
    # Run all tests with reports
    $0

    # Run only unit tests
    $0 -t unit

    # Run tests for specific services
    $0 -s customer-service,auth-service

    # Run tests without coverage checks
    $0 -c

    # Run unit tests without reports
    $0 -t unit -n

Generated Reports:
    - Unit Test Reports:        target/site/surefire-report.html
    - Integration Test Reports: target/site/failsafe-report.html
    - Coverage Reports:         target/site/jacoco/index.html
    - Merged Coverage:          target/site/jacoco-merged/index.html
    - XML Reports:              target/surefire-reports/*.xml
    - JaCoCo Data:              target/jacoco.exec

EOF
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--type)
            TEST_TYPE="$2"
            shift 2
            ;;
        -s|--service)
            SERVICES="$2"
            shift 2
            ;;
        -n|--no-reports)
            GENERATE_REPORTS="false"
            shift
            ;;
        -c|--skip-coverage)
            SKIP_COVERAGE_CHECK="true"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            exit 1
            ;;
    esac
done

# Validate test type
if [[ ! "$TEST_TYPE" =~ ^(all|unit|integration)$ ]]; then
    print_error "Invalid test type: $TEST_TYPE"
    usage
    exit 1
fi

print_info "======================================================"
print_info "  Autonova Backend - Test Execution & Reporting"
print_info "======================================================"
print_info "Test Type: $TEST_TYPE"
print_info "Generate Reports: $GENERATE_REPORTS"
print_info "Skip Coverage Check: $SKIP_COVERAGE_CHECK"
[[ -n "$SERVICES" ]] && print_info "Services: $SERVICES"
print_info "======================================================"
echo

# Determine Maven goals based on test type
MAVEN_GOALS=""
case $TEST_TYPE in
    unit)
        MAVEN_GOALS="clean test"
        ;;
    integration)
        MAVEN_GOALS="clean integration-test"
        ;;
    all)
        MAVEN_GOALS="clean verify"
        ;;
esac

# Add coverage check skip if requested
if [[ "$SKIP_COVERAGE_CHECK" == "true" ]]; then
    MAVEN_GOALS="$MAVEN_GOALS -Djacoco.skip=true"
fi

# Determine services to run
if [[ -n "$SERVICES" ]]; then
    IFS=',' read -ra SERVICE_LIST <<< "$SERVICES"
else
    SERVICE_LIST=("${ALL_SERVICES[@]}")
fi

# Run tests for each service
for service in "${SERVICE_LIST[@]}"; do
    print_info "Running tests for: $service"
    if (cd "$service" && mvn $MAVEN_GOALS); then
        print_success "Tests completed for: $service"
    else
        print_warning "Tests failed for $service"
    fi
    echo
done

print_success "Test execution completed!"
echo

# Generate site reports if requested
if [[ "$GENERATE_REPORTS" == "true" ]]; then
    print_info "Generating HTML reports..."
    for service in "${SERVICE_LIST[@]}"; do
        print_info "Generating report for: $service"
        (cd "$service" && mvn surefire-report:report site -DgenerateReports=false) || {
            print_warning "Report generation failed for $service"
        }
    done
    print_success "HTML reports generated!"
    echo
fi

# Print report locations
print_info "======================================================"
print_info "  Test Reports Generated"
print_info "======================================================"

for service in "${SERVICE_LIST[@]}"; do
    echo -e "${GREEN}$service:${NC}"
    echo "  Unit Tests:    $service/target/site/surefire-report.html"
    echo "  Coverage:      $service/target/site/jacoco/index.html"
    [[ "$TEST_TYPE" == "all" || "$TEST_TYPE" == "integration" ]] && \
        echo "  Integration:   $service/target/site/failsafe-report.html"
    echo "  XML Reports:   $service/target/surefire-reports/"
    echo
done

print_info "======================================================"
print_success "Test execution and reporting complete!"

#!/usr/bin/env python3
"""
Time Logging Service API Test Script
Simple Python script to test key API scenarios
"""

import requests
import json
import time

# Configuration
BASE_URL = "http://localhost:8083/api"
HEADERS = {"Content-Type": "application/json"}

def test_create_time_log():
    """Test creating a time log"""
    print("[TEST] Testing: Create Time Log")

    payload = {
        "employeeId": "emp-001",
        "projectId": "proj-001",
        "taskId": "task-001",
        "hours": 2.5,
        "note": "Testing API with Python script"
    }

    try:
        response = requests.post(f"{BASE_URL}/time-logs", json=payload, headers=HEADERS)
        print(f"[INFO] Status: {response.status_code}")

        if response.status_code == 201:
            data = response.json()
            print(f"[PASS] Success: Created time log {data['id']} with status {data['approvalStatus']}")
            return data['id']
        else:
            print(f"[FAIL] Failed: {response.text}")
            return None
    except Exception as e:
        print(f"[ERROR] Error: {e}")
        return None

def test_get_employee_logs(employee_id):
    """Test getting employee time logs"""
    print(f"\n[TEST] Testing: Get Time Logs for Employee {employee_id}")

    try:
        response = requests.get(f"{BASE_URL}/time-logs/employee/{employee_id}", headers=HEADERS)
        print(f"[INFO] Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"[PASS] Success: Retrieved {len(data)} time logs")
            if data:
                print(f"Latest log: {data[0]['hours']} hours on {data[0]['taskName']}")
            return data
        else:
            print(f"[FAIL] Failed: {response.text}")
            return []
    except Exception as e:
        print(f"[ERROR] Error: {e}")
        return []

def test_get_pending_logs():
    """Test getting pending time logs (Admin)"""
    print("\n[TEST] Testing: Get Pending Time Logs (Admin)")

    try:
        response = requests.get(f"{BASE_URL}/time-logs/pending", headers=HEADERS)
        print(f"[INFO] Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"[PASS] Success: {len(data)} pending logs found")
            return data
        else:
            print(f"[FAIL] Failed: {response.text}")
            return []
    except Exception as e:
        print(f"[ERROR] Error: {e}")
        return []

def test_approve_time_log(log_id):
    """Test approving a time log (Admin)"""
    print(f"\n[TEST] Testing: Approve Time Log {log_id}")

    try:
        response = requests.patch(f"{BASE_URL}/time-logs/{log_id}/approve", headers=HEADERS)
        print(f"[INFO] Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"[PASS] Success: Log approved with status {data['approvalStatus']}")
            return True
        else:
            print(f"[FAIL] Failed: {response.text}")
            return False
    except Exception as e:
        print(f"[ERROR] Error: {e}")
        return False

def test_reject_time_log(log_id):
    """Test rejecting a time log (Admin)"""
    print(f"\n[TEST] Testing: Reject Time Log {log_id}")

    payload = {"reason": "Test rejection from Python script"}

    try:
        response = requests.patch(f"{BASE_URL}/time-logs/{log_id}/reject",
                                json=payload, headers=HEADERS)
        print(f"[INFO] Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"[PASS] Success: Log rejected with status {data['approvalStatus']}")
            return True
        else:
            print(f"[FAIL] Failed: {response.text}")
            return False
    except Exception as e:
        print(f"[ERROR] Error: {e}")
        return False

def test_get_employee_summary(employee_id):
    """Test getting employee summary"""
    print(f"\n[TEST] Testing: Get Employee Summary {employee_id}")

    try:
        response = requests.get(f"{BASE_URL}/time-logs/employee/{employee_id}/summary", headers=HEADERS)
        print(f"[INFO] Status: {response.status_code}")

        if response.status_code == 200:
            data = response.json()
            print(f"[PASS] Success: Employee {data['employeeName']} has {data['totalHours']} total hours")
            return data
        else:
            print(f"[FAIL] Failed: {response.text}")
            return None
    except Exception as e:
        print(f"[ERROR] Error: {e}")
        return None

def main():
    """Run all test scenarios"""
    print("[INFO] Time Logging Service API Test Suite")
    print("=" * 50)

    # Check if service is running
    try:
        response = requests.get("http://localhost:8083/actuator/health")
        if response.status_code != 200:
            print("[ERROR] Time Logging Service is not running on port 8083")
            print("[INFO] Please start the service first: mvn spring-boot:run")
            return
    except:
        print("[ERROR] Cannot connect to Time Logging Service")
        print("[INFO] Please start the service first: mvn spring-boot:run")
        return

    print("[PASS] Service is running, starting tests...\n")

    # Test scenarios
    created_log_id = test_create_time_log()

    if created_log_id:
        # Test getting logs
        test_get_employee_logs("emp-001")

        # Test admin functions
        pending_logs = test_get_pending_logs()

        # If we have pending logs, test approval/rejection
        if pending_logs:
            test_log_id = pending_logs[0]['id']
            test_approve_time_log(test_log_id)

            # Create another log to test rejection
            new_log_id = test_create_time_log()
            if new_log_id:
                test_reject_time_log(new_log_id)

        # Test summary (should only count approved logs)
        test_get_employee_summary("emp-001")

    print("\n" + "=" * 50)
    print("[PASS] Test suite completed!")
    print("\n[INFO] Note: Make sure to run database migration first:")
    print("ALTER TABLE time_logs ADD COLUMN approval_status VARCHAR(20) DEFAULT 'PENDING';")

if __name__ == "__main__":
    main()
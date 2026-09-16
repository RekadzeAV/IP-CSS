#!/bin/bash
# NAS Platforms Testing Script
# Tests all NAS platform integrations

set -e

echo "=== IP-CSS NAS Platforms Testing ==="
echo "Date: $(date)"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

# Test functions

test_synology_integration() {
    echo -e "\n${YELLOW}Testing Synology Integration...${NC}"
    
    # Check if running on Synology
    if [ -f "/etc.defaults/VERSION" ]; then
        echo "  ✓ Running on Synology DSM"
        
        # Test synonotify
        if [ -x "/usr/syno/bin/synonotify" ]; then
            echo "  ✓ synonotify available"
            ((TESTS_PASSED++))
        else
            echo "  ✗ synonotify not available"
            ((TESTS_FAILED++))
        fi
        
        # Test resource monitoring
        if [ -f "/proc/stat" ]; then
            CPU_USAGE=$(grep 'cpu ' /proc/stat | awk '{usage=($2+$4)*100/($2+$4+$5)} END {print usage}')
            echo "  ✓ CPU monitoring working (Usage: ${CPU_USAGE}%)"
            ((TESTS_PASSED++))
        else
            echo "  ✗ CPU monitoring failed"
            ((TESTS_FAILED++))
        fi
        
        # Test disk monitoring
        if command -v df &> /dev/null; then
            DISK_USAGE=$(df -h /volume1 | tail -1 | awk '{print $5}')
            echo "  ✓ Disk monitoring working (Usage: ${DISK_USAGE})"
            ((TESTS_PASSED++))
        else
            echo "  ✗ Disk monitoring failed"
            ((TESTS_FAILED++))
        fi
        
        # Test temperature (if available)
        TEMP_FILE=$(ls /sys/class/hwmon/hwmon*/temp*_input 2>/dev/null | head -1)
        if [ -n "$TEMP_FILE" ] && [ -f "$TEMP_FILE" ]; then
            TEMP=$(cat "$TEMP_FILE" | awk '{printf "%.1f", $1/1000}')
            echo "  ✓ Temperature monitoring working (${TEMP}°C)"
            ((TESTS_PASSED++))
        else
            echo "  ⚠ Temperature monitoring not available"
            ((TESTS_SKIPPED++))
        fi
        
    else
        echo "  ⚠ Not running on Synology - skipping tests"
        ((TESTS_SKIPPED+=5))
    fi
}

test_qnap_integration() {
    echo -e "\n${YELLOW}Testing QNAP Integration...${NC}"
    
    # Check if running on QNAP
    if [ -f "/etc/config/qpkg.conf" ]; then
        echo "  ✓ Running on QNAP QTS"
        
        # Test qnotify
        if [ -x "/usr/bin/qnotify" ]; then
            echo "  ✓ qnotify available"
            ((TESTS_PASSED++))
        else
            echo "  ✗ qnotify not available"
            ((TESTS_FAILED++))
        fi
        
        # Test getcfg
        if command -v getcfg &> /dev/null; then
            MODEL=$(getcfg System "Model" -f /etc/config/uLinux.conf 2>/dev/null || echo "Unknown")
            echo "  ✓ getcfg working (Model: ${MODEL})"
            ((TESTS_PASSED++))
        else
            echo "  ✗ getcfg failed"
            ((TESTS_FAILED++))
        fi
        
        # Test resource monitoring
        if [ -f "/proc/meminfo" ]; then
            MEM_TOTAL=$(grep MemTotal /proc/meminfo | awk '{print $2}')
            MEM_FREE=$(grep MemAvailable /proc/meminfo | awk '{print $2}')
            MEM_USAGE=$(awk "BEGIN {printf \"%.1f\", (${MEM_TOTAL}-${MEM_FREE})/${MEM_TOTAL}*100}")
            echo "  ✓ Memory monitoring working (Usage: ${MEM_USAGE}%)"
            ((TESTS_PASSED++))
        else
            echo "  ✗ Memory monitoring failed"
            ((TESTS_FAILED++))
        fi
        
    else
        echo "  ⚠ Not running on QNAP - skipping tests"
        ((TESTS_SKIPPED+=4))
    fi
}

test_asustor_integration() {
    echo -e "\n${YELLOW}Testing Asustor Integration...${NC}"
    
    # Check if running on Asustor
    if [ -d "/usr/local/IP-CSS" ] || [ -f "/etc/config/mtab" ]; then
        echo "  ✓ Running on Asustor ADM"
        
        # Test actrl
        if [ -x "/usr/bin/actrl" ]; then
            echo "  ✓ actrl available"
            ((TESTS_PASSED++))
        else
            echo "  ⚠ actrl not available"
            ((TESTS_SKIPPED++))
        fi
        
        # Test resource monitoring
        if [ -f "/proc/stat" ]; then
            echo "  ✓ Resource monitoring available"
            ((TESTS_PASSED++))
        else
            echo "  ✗ Resource monitoring failed"
            ((TESTS_FAILED++))
        fi
        
    else
        echo "  ⚠ Not running on Asustor - skipping tests"
        ((TESTS_SKIPPED+=3))
    fi
}

test_truenas_docker() {
    echo -e "\n${YELLOW}Testing TrueNAS Docker Integration...${NC}"
    
    # Check if running in Docker
    if [ -f "/.dockerenv" ]; then
        echo "  ✓ Running in Docker container"
        
        # Test Docker health check
        if command -v curl &> /dev/null; then
            HEALTH_RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health 2>/dev/null || echo "000")
            if [ "$HEALTH_RESPONSE" = "200" ]; then
                echo "  ✓ Health check endpoint responding"
                ((TESTS_PASSED++))
            else
                echo "  ✗ Health check endpoint not responding (${HEALTH_RESPONSE})"
                ((TESTS_FAILED++))
            fi
        else
            echo "  ⚠ curl not available for health check"
            ((TESTS_SKIPPED++))
        fi
        
        # Test volume mounts
        if [ -d "/data" ]; then
            echo "  ✓ Data volume mounted"
            ((TESTS_PASSED++))
        else
            echo "  ✗ Data volume not mounted"
            ((TESTS_FAILED++))
        fi
        
        # Test GPU passthrough (if configured)
        if [ -e "/dev/dri" ]; then
            echo "  ✓ GPU device available"
            ((TESTS_PASSED++))
        else
            echo "  ⚠ GPU device not available"
            ((TESTS_SKIPPED++))
        fi
        
    else
        echo "  ⚠ Not running in Docker - skipping tests"
        ((TESTS_SKIPPED+=4))
    fi
}

test_hardware_acceleration() {
    echo -e "\n${YELLOW}Testing Hardware Acceleration...${NC}"
    
    # Test Intel QuickSync
    if [ -e "/dev/dri/renderD128" ]; then
        echo "  ✓ Intel QuickSync device available"
        
        if command -v vainfo &> /dev/null; then
            VAINFO_OUTPUT=$(vainfo 2>&1)
            if echo "$VAINFO_OUTPUT" | grep -q "VAProfileH264"; then
                echo "  ✓ H.264 encoding supported"
                ((TESTS_PASSED++))
            else
                echo "  ⚠ H.264 encoding not supported"
                ((TESTS_SKIPPED++))
            fi
            
            if echo "$VAINFO_OUTPUT" | grep -q "VAProfileH265"; then
                echo "  ✓ H.265 encoding supported"
                ((TESTS_PASSED++))
            else
                echo "  ⚠ H.265 encoding not supported"
                ((TESTS_SKIPPED++))
            fi
        else
            echo "  ⚠ vainfo not available"
            ((TESTS_SKIPPED+=2))
        fi
    else
        echo "  ⚠ Intel QuickSync not available"
        ((TESTS_SKIPPED+=3))
    fi
    
    # Test NVIDIA NVENC
    if [ -e "/dev/nvidia0" ]; then
        echo "  ✓ NVIDIA GPU device available"
        
        if command -v nvidia-smi &> /dev/null; then
            NVIDIA_OUTPUT=$(nvidia-smi --query-gpu=name,driver_version --format=csv,noheader 2>&1)
            if [ $? -eq 0 ]; then
                echo "  ✓ NVIDIA driver working (${NVIDIA_OUTPUT})"
                ((TESTS_PASSED++))
            else
                echo "  ✗ NVIDIA driver not working"
                ((TESTS_FAILED++))
            fi
        else
            echo "  ⚠ nvidia-smi not available"
            ((TESTS_SKIPPED++))
        fi
    else
        echo "  ⚠ NVIDIA GPU not available"
        ((TESTS_SKIPPED+=2))
    fi
    
    # Test AMD VCE
    if [ -e "/dev/dri/renderD128" ] && [ -d "/sys/module/amdgpu" ]; then
        echo "  ✓ AMD GPU device available"
        
        if command -v vainfo &> /dev/null; then
            VAINFO_OUTPUT=$(vainfo 2>&1)
            if echo "$VAINFO_OUTPUT" | grep -q "VAProfileH264"; then
                echo "  ✓ AMD VCE H.264 encoding supported"
                ((TESTS_PASSED++))
            else
                echo "  ⚠ AMD VCE H.264 encoding not supported"
                ((TESTS_SKIPPED++))
            fi
        fi
    else
        echo "  ⚠ AMD GPU not available"
        ((TESTS_SKIPPED+=2))
    fi
    
    # Test ARM Mali
    if [ -e "/dev/mali0" ] || [ -e "/dev/video10" ]; then
        echo "  ✓ ARM Mali VPU device available"
        ((TESTS_PASSED++))
    else
        echo "  ⚠ ARM Mali VPU not available"
        ((TESTS_SKIPPED++))
    fi
}

test_package_structure() {
    echo -e "\n${YELLOW}Testing Package Structure...${NC}"
    
    # Test Synology SPK structure
    if [ -d "platforms/nas-synology/package" ]; then
        echo "  ✓ Synology package structure exists"
        
        if [ -f "platforms/nas-synology/package/INFO" ]; then
            echo "  ✓ Synology INFO file exists"
            ((TESTS_PASSED++))
        else
            echo "  ✗ Synology INFO file missing"
            ((TESTS_FAILED++))
        fi
        
        if [ -f "platforms/nas-synology/package/scripts/start-stop-status" ]; then
            echo "  ✓ Synology start-stop-status script exists"
            ((TESTS_PASSED++))
        else
            echo "  ✗ Synology start-stop-status script missing"
            ((TESTS_FAILED++))
        fi
    else
        echo "  ✗ Synology package structure missing"
        ((TESTS_FAILED+=2))
    fi
    
    # Test QNAP QPKG structure
    if [ -d "platforms/nas-qnap/QPKG" ]; then
        echo "  ✓ QNAP package structure exists"
        
        if [ -f "platforms/nas-qnap/QPKG/qpkg.cfg" ]; then
            echo "  ✓ QNAP qpkg.cfg file exists"
            ((TESTS_PASSED++))
        else
            echo "  ✗ QNAP qpkg.cfg file missing"
            ((TESTS_FAILED++))
        fi
    else
        echo "  ✗ QNAP package structure missing"
        ((TESTS_FAILED++))
    fi
    
    # Test Asustor APK structure
    if [ -d "platforms/nas-asustor/package" ]; then
        echo "  ✓ Asustor package structure exists"
        ((TESTS_PASSED++))
    else
        echo "  ✗ Asustor package structure missing"
        ((TESTS_FAILED++))
    fi
    
    # Test TrueNAS Docker structure
    if [ -f "platforms/nas-truenas/docker/Dockerfile" ]; then
        echo "  ✓ TrueNAS Dockerfile exists"
        ((TESTS_PASSED++))
    else
        echo "  ✗ TrueNAS Dockerfile missing"
        ((TESTS_FAILED++))
    fi
    
    if [ -f "platforms/nas-truenas/helm/ip-css/Chart.yaml" ]; then
        echo "  ✓ TrueNAS Helm chart exists"
        ((TESTS_PASSED++))
    else
        echo "  ✗ TrueNAS Helm chart missing"
        ((TESTS_FAILED++))
    fi
}

# Run all tests

echo "Starting NAS Platforms Tests..."
echo "================================"

test_synology_integration
test_qnap_integration
test_asustor_integration
test_truenas_docker
test_hardware_acceleration
test_package_structure

# Print summary

echo ""
echo "================================"
echo -e "${YELLOW}Test Summary:${NC}"
echo -e "  Passed:  ${GREEN}${TESTS_PASSED}${NC}"
echo -e "  Failed:  ${RED}${TESTS_FAILED}${NC}"
echo -e "  Skipped: ${YELLOW}${TESTS_SKIPPED}${NC}"
echo ""

TOTAL=$((TESTS_PASSED + TESTS_FAILED))
if [ $TOTAL -gt 0 ]; then
    PASS_RATE=$(awk "BEGIN {printf \"%.1f\", ${TESTS_PASSED}/${TOTAL}*100}")
    echo -e "Pass Rate: ${GREEN}${PASS_RATE}%${NC}"
fi

echo ""
if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed!${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed!${NC}"
    exit 1
fi

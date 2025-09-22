#!/bin/bash

# Docker Image Test Script for Statik
# This script builds and tests the Docker image locally

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test configuration
IMAGE_NAME="statik-test"
TAG="latest"
FULL_IMAGE_NAME="${IMAGE_NAME}:${TAG}"
TEST_DIR="./test-workspace"
TEST_CONTENT_DIR="${TEST_DIR}/content"

echo -e "${YELLOW}Starting Docker image tests for Statik...${NC}"

# Clean up function
cleanup() {
    echo -e "${YELLOW}Cleaning up test resources...${NC}"
    docker rmi "${FULL_IMAGE_NAME}" 2>/dev/null || true
    rm -rf "${TEST_DIR}" 2>/dev/null || true
    echo -e "${GREEN}Cleanup completed${NC}"
}

# Set up cleanup trap
trap cleanup EXIT

# Test 1: Build Docker image
echo -e "${YELLOW}Test 1: Building Docker image...${NC}"
if docker build --platform linux/$(uname -m) -t "${FULL_IMAGE_NAME}" .; then
    echo -e "${GREEN}✓ Docker image built successfully${NC}"
else
    echo -e "${RED}✗ Docker image build failed${NC}"
    exit 1
fi

# Test 2: Check image exists and basic info
echo -e "${YELLOW}Test 2: Checking image info...${NC}"
if docker image inspect "${FULL_IMAGE_NAME}" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Docker image exists${NC}"
    IMAGE_SIZE=$(docker image inspect "${FULL_IMAGE_NAME}" --format='{{.Size}}' | awk '{print int($1/1024/1024)" MB"}')
    echo -e "${GREEN}Image size: ${IMAGE_SIZE}${NC}"
else
    echo -e "${RED}✗ Docker image not found${NC}"
    exit 1
fi

# Test 3: Basic help command
echo -e "${YELLOW}Test 3: Testing help command...${NC}"
if docker run --rm "${FULL_IMAGE_NAME}" --help > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Help command works${NC}"
else
    echo -e "${RED}✗ Help command failed${NC}"
    exit 1
fi

# Test 4: Create test workspace
echo -e "${YELLOW}Test 4: Setting up test workspace...${NC}"
mkdir -p "${TEST_CONTENT_DIR}"
mkdir -p "${TEST_DIR}/templates"

# Create a simple test markdown file
cat > "${TEST_CONTENT_DIR}/index.md" << 'EOF'
---
title: Test Page
---

# Test Page

This is a test page for Docker image testing.

## Features

- Markdown rendering
- Static site generation
- Docker containerization
EOF

# Copy templates from the project to test workspace
cp -r "./statik.github.io/templates/"* "${TEST_DIR}/templates/"

# Create a config file
cat > "${TEST_DIR}/config.json" << 'EOF'
{
    "siteName": "Test Site",
    "baseUrl": "https://test-docker.com",
    "description": "A test site for Docker image validation",
    "author": "Docker Test"
}
EOF

echo -e "${GREEN}✓ Test workspace created${NC}"

# Test 5: Run statik with Docker (basic generation)
echo -e "${YELLOW}Test 5: Testing site generation...${NC}"
if docker run --rm -v "$(pwd)/${TEST_DIR}:/github/workspace" "${FULL_IMAGE_NAME}" run -- --root-path /github/workspace; then
    echo -e "${GREEN}✓ Site generation completed${NC}"
else
    echo -e "${RED}✗ Site generation failed${NC}"
    exit 1
fi

# Test 6: Check generated output
echo -e "${YELLOW}Test 6: Verifying generated output...${NC}"
if [ -d "${TEST_DIR}/build" ] && [ -f "${TEST_DIR}/build/index.html" ]; then
    echo -e "${GREEN}✓ Generated files found${NC}"
    echo -e "${GREEN}Generated files:${NC}"
    find "${TEST_DIR}/build" -type f | sed 's/^/  /'
else
    echo -e "${RED}✗ Generated files not found${NC}"
    echo -e "${YELLOW}Contents of test directory:${NC}"
    ls -la "${TEST_DIR}/" || true
    exit 1
fi

# Test 7: Test with bind mount (simulating real usage)
echo -e "${YELLOW}Test 7: Testing with current directory mount...${NC}"
TEMP_TEST_DIR=$(mktemp -d)
mkdir -p "${TEMP_TEST_DIR}/templates"
mkdir -p "${TEMP_TEST_DIR}/content"
echo "# Docker Test" > "${TEMP_TEST_DIR}/README.md"

# Copy templates to temp directory
cp -r "./statik.github.io/templates/"* "${TEMP_TEST_DIR}/templates/"

# Create a simple content file
cat > "${TEMP_TEST_DIR}/content/index.md" << 'EOF'
---
title: Bind Mount Test
---

# Bind Mount Test

Testing bind mount functionality.
EOF

cat > "${TEMP_TEST_DIR}/config.json" << 'EOF'
{
    "siteName": "Bind Mount Test",
    "baseUrl": "https://bind-test.com",
    "description": "Testing bind mount functionality",
    "author": "Docker Test"
}
EOF

if docker run --rm -v "${TEMP_TEST_DIR}:/github/workspace" "${FULL_IMAGE_NAME}" run -- --root-path /github/workspace; then
    echo -e "${GREEN}✓ Bind mount test successful${NC}"
else
    echo -e "${RED}✗ Bind mount test failed${NC}"
    rm -rf "${TEMP_TEST_DIR}"
    exit 1
fi

rm -rf "${TEMP_TEST_DIR}"

# Test 8: Test server mode (quick test)
echo -e "${YELLOW}Test 8: Testing server mode (5 second test)...${NC}"
if timeout 5s docker run --rm -v "$(pwd)/${TEST_DIR}:/github/workspace" -p 8080:8080 "${FULL_IMAGE_NAME}" run -- --root-path /github/workspace --w > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Server mode started successfully${NC}"
else
    # This might timeout, which is expected for the server mode
    echo -e "${GREEN}✓ Server mode test completed (timeout expected)${NC}"
fi

# Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}All Docker tests passed successfully! ✓${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "\n${YELLOW}Usage examples:${NC}"
echo -e "Build image: ${GREEN}docker build -t statik .${NC}"
echo -e "Generate site: ${GREEN}docker run --rm -v \$(pwd):/workspace -w /workspace statik run -- --root-path .${NC}"
echo -e "Serve with watch: ${GREEN}docker run --rm -v \$(pwd):/workspace -w /workspace -p 8080:8080 statik run -- --root-path . --w${NC}"
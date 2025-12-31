#!/bin/bash
#
# Build jsnippets container image with dynamic OCI metadata labels.
#
# Usage:
#   ./build-image.sh              # Build with auto-detected values (prunes dangling images)
#   ./build-image.sh --push       # Build and push to registry
#   ./build-image.sh --no-cache   # Build without any Docker cache
#   ./build-image.sh --no-prune   # Skip pruning dangling images after build
#   ./build-image.sh --clean      # Full cleanup: prune + remove build cache
#
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Image name
IMAGE_NAME="jsnippets"

# Parse command line arguments
PUSH=false
NO_CACHE=""
PRUNE=true  # Prune by default
CLEAN=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --push)
            PUSH=true
            shift
            ;;
        --no-cache)
            NO_CACHE="--no-cache"
            shift
            ;;
        --no-prune)
            PRUNE=false
            shift
            ;;
        --prune)
            # Keep for backwards compatibility, but it's now the default
            PRUNE=true
            shift
            ;;
        --clean)
            CLEAN=true
            shift
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Detect container runtime (podman or docker)
if command -v podman &> /dev/null; then
    CONTAINER_CMD="podman"
elif command -v docker &> /dev/null; then
    CONTAINER_CMD="docker"
else
    echo -e "${RED}Error: Neither podman nor docker found in PATH${NC}"
    exit 1
fi

echo -e "${GREEN}Using container runtime: ${CONTAINER_CMD}${NC}"

# Get build metadata
BUILD_DATE=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Extract version from pom.xml
if [[ -f "pom.xml" ]]; then
    APP_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout 2>/dev/null || echo "unknown")
else
    APP_VERSION="unknown"
fi

# Get git revision
if command -v git &> /dev/null && git rev-parse --git-dir &> /dev/null; then
    VCS_REF=$(git rev-parse --short HEAD 2>/dev/null || echo "unknown")
    # Check for uncommitted changes
    if [[ -n $(git status --porcelain 2>/dev/null) ]]; then
        VCS_REF="${VCS_REF}-dirty"
    fi
else
    VCS_REF="unknown"
fi

# Compute hash of source files to bust cache when source changes
# This ensures the build step runs even if Docker's COPY cache doesn't detect changes
if command -v find &> /dev/null && command -v shasum &> /dev/null; then
    SRC_HASH=$(find src -type f \( -name "*.java" -o -name "*.properties" -o -name "*.html" -o -name "*.xml" -o -name "*.yaml" -o -name "*.yml" \) 2>/dev/null | sort | xargs cat 2>/dev/null | shasum -a 256 | cut -c1-12)
elif command -v find &> /dev/null && command -v md5sum &> /dev/null; then
    SRC_HASH=$(find src -type f \( -name "*.java" -o -name "*.properties" -o -name "*.html" -o -name "*.xml" -o -name "*.yaml" -o -name "*.yml" \) 2>/dev/null | sort | xargs cat 2>/dev/null | md5sum | cut -c1-12)
else
    # Fallback to timestamp if hash tools unavailable
    SRC_HASH=$(date +%s)
fi

echo -e "${YELLOW}Building ${IMAGE_NAME}:${APP_VERSION}${NC}"
echo -e "  Build Date: ${BUILD_DATE}"
echo -e "  Version:    ${APP_VERSION}"
echo -e "  Git Ref:    ${VCS_REF}"
echo -e "  Src Hash:   ${SRC_HASH}"
echo ""

# Build the image
${CONTAINER_CMD} build \
    ${NO_CACHE} \
    --build-arg BUILD_DATE="${BUILD_DATE}" \
    --build-arg APP_VERSION="${APP_VERSION}" \
    --build-arg VCS_REF="${VCS_REF}" \
    --build-arg CACHEBUST="${SRC_HASH}" \
    -t "${IMAGE_NAME}:${APP_VERSION}" \
    -t "${IMAGE_NAME}:latest" \
    .

echo ""
echo -e "${GREEN}Successfully built:${NC}"
echo -e "  ${IMAGE_NAME}:${APP_VERSION}"
echo -e "  ${IMAGE_NAME}:latest"

# Show image labels
echo ""
echo -e "${YELLOW}Image labels:${NC}"
${CONTAINER_CMD} inspect "${IMAGE_NAME}:latest" --format '{{range $k, $v := .Config.Labels}}{{$k}}={{$v}}{{"\n"}}{{end}}' | grep "org.opencontainers" | sort

# Push if requested
if [[ "${PUSH}" == "true" ]]; then
    echo ""
    echo -e "${YELLOW}Pushing images...${NC}"
    ${CONTAINER_CMD} push "${IMAGE_NAME}:${APP_VERSION}"
    ${CONTAINER_CMD} push "${IMAGE_NAME}:latest"
    echo -e "${GREEN}Push complete${NC}"
fi

# Cleanup if requested
if [[ "${PRUNE}" == "true" ]]; then
    echo ""
    echo -e "${YELLOW}Pruning dangling images...${NC}"
    ${CONTAINER_CMD} image prune -f
    echo -e "${GREEN}Dangling images removed${NC}"
fi

if [[ "${CLEAN}" == "true" ]]; then
    echo ""
    echo -e "${YELLOW}Cleaning build cache...${NC}"
    ${CONTAINER_CMD} builder prune -f 2>/dev/null || ${CONTAINER_CMD} system prune -f --filter "until=24h"
    echo -e "${GREEN}Build cache cleaned${NC}"
fi


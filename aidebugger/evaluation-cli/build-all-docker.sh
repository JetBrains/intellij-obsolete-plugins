#!/usr/bin/env bash
set -euo pipefail

# Build fat JAR with Bazel (must run from monorepo root)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
echo "Building evaluation-cli fat JAR with Bazel..."
"$REPO_ROOT/bazel.cmd" build //plugins/aidebugger/evaluation-cli:evaluation-cli-bin_deploy.jar
JAR_DIR="$REPO_ROOT/out/bazel-bin/plugins/aidebugger/evaluation-cli"

# Docker build context is plugins/aidebugger/; JAR injected via --build-context
cd "$SCRIPT_DIR/.."

# Configuration
REGISTRY="registry.jetbrains.team/p/mlops/ai-toolkit"
BUILDER="multiarch-builder"
VERSION="${VERSION:-$(date +%Y%m%d-%H%M%S)}"
LATEST_TAG="latest"

# Image definitions - bash 3.x compatible
NAMES=(
    "eval"
    "eval-py-3.10"
    "eval-py-3.11"
    "eval-py-3.12"
    "eval-py-3.13"
    "eval-py-3.14"
)

DOCKERFILES=(
    "evaluation-cli/Dockerfile"
    "evaluation-cli/docker_py_10/Dockerfile"
    "evaluation-cli/docker_py_11/Dockerfile"
    "evaluation-cli/docker_py_12/Dockerfile"
    "evaluation-cli/docker_py_13/Dockerfile"
    "evaluation-cli/docker_py_14/Dockerfile"
)

# Create/reuse buildx builder
echo "Creating/reusing buildx builder '$BUILDER'..."
docker buildx create --name "$BUILDER" --use --driver docker-container || true
docker buildx inspect "$BUILDER" --bootstrap

build_image() {
    local name="$1"
    local dockerfile="$2"

    local full_name="$REGISTRY/$name"
    local version_tag="$full_name:$VERSION"
    local latest_tag="$full_name:$LATEST_TAG"

    echo "🚀 Building $name ($version_tag) from $dockerfile..."

    docker buildx build \
        --platform linux/amd64,linux/arm64 \
        --build-context "jar=$JAR_DIR" \
        -t "$version_tag" \
        -t "$latest_tag" \
        -f "$dockerfile" \
        --push \
        .

    echo "✅ Pushed $name: $version_tag & $latest_tag"
}

# Build all
echo "Starting parallel builds for version $VERSION..."
for i in "${!NAMES[@]}"; do
    name="${NAMES[$i]}"
    dockerfile="${DOCKERFILES[$i]}"
    build_image "$name" "$dockerfile" &
done

wait
echo "🎉 All images built and pushed successfully!"
echo "Version tag: $VERSION"
echo "Images:"
for i in "${!NAMES[@]}"; do
    echo "  - $REGISTRY/${NAMES[$i]}:$VERSION"
done

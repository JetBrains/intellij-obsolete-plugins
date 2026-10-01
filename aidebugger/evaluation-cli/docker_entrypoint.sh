#!/usr/bin/env bash
set -euo pipefail

export PYTHONPATH="/workspace${PYTHONPATH:+:${PYTHONPATH}}"

CONTAINER_VENV="/home/runner/app/.venv"

PYTHON_VERSION="${PYTHON_VERSION:-3.11.2}"

export PYENV_ROOT="/home/runner/.pyenv"
export PATH="$PYENV_ROOT/bin:$PYENV_ROOT/shims:$PATH"

if command -v pyenv >/dev/null 2>&1; then
    PREFIX=$(echo "$PYTHON_VERSION" | cut -d. -f1-2)
    AVAILABLE=$(pyenv versions --bare | grep "^$PREFIX\." | sort -V | tail -n 1)
    if [ -n "$AVAILABLE" ]; then
      PYTHON_VERSION="$AVAILABLE"
    else
        echo "[entrypoint] Installing Python $PYTHON_VERSION via pyenv..."
        pyenv install -s "$PYTHON_VERSION"
    fi
    PY_EXEC=$(pyenv prefix "$PYTHON_VERSION")/bin/python
else
    PY_EXEC=$(command -v python3)
    echo "[entrypoint] pyenv not found, using system python: $PY_EXEC"
fi

if [[ ! -d "$CONTAINER_VENV" ]]; then
    echo "[entrypoint] Creating container venv at $CONTAINER_VENV with Python $PYTHON_VERSION"
    "$PY_EXEC" -m venv "$CONTAINER_VENV"
fi

source "$CONTAINER_VENV/bin/activate"

export PATH="$CONTAINER_VENV/bin:$PATH"

python -m pip install --upgrade pip setuptools wheel

echo "[entrypoint] Using container venv Python: $(which python)"
python --version

PYTHON_ENV_TOOL="${PYTHON_ENV_TOOL:-}"
PYTHON_ENV_CONFIG_PATH="${PYTHON_ENV_CONFIG_PATH:-}"

echo "[entrypoint] PYTHON_ENV_TOOL='$PYTHON_ENV_TOOL'"
echo "[entrypoint] PYTHON_ENV_CONFIG_PATH='$PYTHON_ENV_CONFIG_PATH'"

if [[ -z "$PYTHON_ENV_TOOL" ]]; then
    echo "[entrypoint] No PYTHON_ENV_TOOL provided; skipping dependency install."
    :

elif [[ "$PYTHON_ENV_TOOL" == "pip" ]]; then
    PIP_INSTALL_CMD="python -m pip install"
    echo "[entrypoint] Installing dependencies via pip from $PYTHON_ENV_CONFIG_PATH"
    $PIP_INSTALL_CMD packaging # required for installing some packages
    $PIP_INSTALL_CMD -r "$PYTHON_ENV_CONFIG_PATH" \
        --extra-index-url https://download.pytorch.org/whl/cu117 \
        --extra-index-url https://download.pytorch.org/whl/cu118 \
        --extra-index-url https://download.pytorch.org/whl/cu121 \
        --extra-index-url https://download.pytorch.org/whl/cu124

elif [[ "$PYTHON_ENV_TOOL" == "poetry" ]]; then
    python -m pip install poetry
    echo "[entrypoint] Installing dependencies via poetry from $PYTHON_ENV_CONFIG_PATH"
    cd "$(dirname "$PYTHON_ENV_CONFIG_PATH")" && poetry install --directory "$PYTHON_ENV_CONFIG_PATH" --no-root

elif [[ "$PYTHON_ENV_TOOL" == "uv" ]]; then
    echo "[entrypoint] Installing dependencies via uv from $PYTHON_ENV_CONFIG_PATH"
    python -m pip install uv
    cd "$(dirname "$PYTHON_ENV_CONFIG_PATH")" && uv sync --all-packages --all-extras --active

elif [[ "$PYTHON_ENV_TOOL" == "pdm" ]]; then
    python -m pip install pdm
    echo "[entrypoint] Installing dependencies via pdm"
    cd "$(dirname "$PYTHON_ENV_CONFIG_PATH")" && pdm install --venv "$CONTAINER_VENV"

elif [[ "$PYTHON_ENV_TOOL" == "hatch" ]]; then
    python -m pip install hatch
    echo "[entrypoint] Installing dependencies via hatch"
    cd "$(dirname "$PYTHON_ENV_CONFIG_PATH")" && hatch env create

else
    echo "[entrypoint] Unknown python env tool $PYTHON_ENV_TOOL"
    exit 1
fi

# Default output directory
OUT_DIR="${OUT_DIR:-/workspace/.jbeval/remote/out}"
mkdir -p "$OUT_DIR"
: > "$OUT_DIR/tracer_outputs.json"

exec java -jar /home/runner/app/evaluation-cli.jar pipeline "$@"
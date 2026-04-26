#!/bin/zsh

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

source "$SCRIPT_DIR/backend-env.sh"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  echo "DB_PASSWORD is required."
  echo "Example: DB_PASSWORD='your-password' $0"
  exit 1
fi

mkdir -p "$REPO_ROOT/.cache"
mkdir -p "$MAVEN_USER_HOME/repository"

cd "$REPO_ROOT/urbanova"
exec mvn -Dmaven.repo.local="$MAVEN_USER_HOME/repository" spring-boot:run

#!/bin/zsh

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)

export JAVA_HOME="$REPO_ROOT/.tools/jdk-17.0.18+8/Contents/Home"
export MAVEN_HOME="$REPO_ROOT/.tools/apache-maven-3.9.9"
export MAVEN_USER_HOME="$REPO_ROOT/.cache/m2"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"
export MAVEN_OPTS="${MAVEN_OPTS:-} -Djava.awt.headless=true"

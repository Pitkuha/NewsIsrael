#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

mvn -q package
exec java -jar target/news-israel-app-1.0.0.jar

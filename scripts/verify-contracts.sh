#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$repo_root/apps/web"
npm ci
npm test -- src/modules/resume/contracts.test.js
npm run build

cd "$repo_root"
mvn -q -pl modules/resume-domain test

#!/usr/bin/env bash
# Generate the 9-feature test docx. Requires python-docx.
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
cd "$HERE/.."   # run from poc-conversion/ so relative path 'fixtures/' matches generate.py
python3 fixtures/generate.py

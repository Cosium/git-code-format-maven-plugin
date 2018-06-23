#!/bin/bash
set -e
# Retrieve staged files
STAGED_FILES_FILE=$(mktemp)
git diff --cached --name-only > "${STAGED_FILES_FILE}"
# Process the files
%s git-code-format:on-pre-commit -DstagedFilesFile=${STAGED_FILES_FILE} %s
# Add the files to staging again in case they were modified by the process
while read file; do
  git add ${file}
done <${STAGED_FILES_FILE}

rm ${STAGED_FILES_FILE}
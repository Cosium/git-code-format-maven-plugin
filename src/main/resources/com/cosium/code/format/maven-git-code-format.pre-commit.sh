#!/bin/bash
# Retrieve staged files
STAGED_FILES=$(git diff --cached --name-only)
# Turn the staged files into a comma separated list
STAGED_COMMA_SEPARATED_FILES=""
for file in ${STAGED_FILES}; do
    STAGED_COMMA_SEPARATED_FILES+="$file,"
done
# Process the files
%s git-code-format:on-pre-commit -DstagedFiles=${STAGED_COMMA_SEPARATED_FILES}
# Add the files to staging again in case they were modified by the process
for file in ${STAGED_FILES}; do
    git add ${file}
done
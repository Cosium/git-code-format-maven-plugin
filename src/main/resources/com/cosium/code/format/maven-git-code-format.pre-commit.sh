#!/bin/bash
STAGED_FILES=$(git diff --cached --name-only)
STAGED_COMMA_SEPARATED_FILES=""
for file in ${STAGED_FILES}; do
    STAGED_COMMA_SEPARATED_FILES+="$file,"
done
%s git-code-format:on-pre-commit -DstagedFiles=${STAGED_COMMA_SEPARATED_FILES}
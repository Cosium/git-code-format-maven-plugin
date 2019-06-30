#!/usr/bin/env bash
set -e

mvn clean test

mvn versions:set -DremoveSnapshot=true
mvn versions:commit 
git add pom.xml

RELEASE_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

git commit -m "Release $RELEASE_VERSION" 
mvn clean deploy -DperformRelease=true -DskipTests
git tag ${RELEASE_VERSION}

mvn versions:set -DnextSnapshot=true
mvn versions:commit 
git add pom.xml

NEW_SNAPSHOT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
git commit -m "Setup $NEW_SNAPSHOT_VERSION"

git push
git push --tags
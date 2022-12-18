#!/usr/bin/env bash
set -e

./mvnw clean test

./mvnw versions:set -DremoveSnapshot=true
./mvnw versions:commit
git add pom.xml

RELEASE_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)

git commit -m "Release $RELEASE_VERSION" 
./mvnw clean deploy -DperformRelease=true -DskipTests
git tag ${RELEASE_VERSION}

./mvnw versions:set -DnextSnapshot=true
./mvnw versions:commit
git add pom.xml

NEW_SNAPSHOT_VERSION=$(./mvnw help:evaluate -Dexpression=project.version -q -DforceStdout)
git commit -m "Setup $NEW_SNAPSHOT_VERSION"

git push
git push --tags

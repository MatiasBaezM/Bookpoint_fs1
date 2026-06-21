#!/usr/bin/env sh
MAVEN_VERSION="${MAVEN_VERSION:-3.9.6}"
BASEDIR=$(dirname "$0")
exec "$BASEDIR/.mvn/wrapper/maven-wrapper.jar" "$@"

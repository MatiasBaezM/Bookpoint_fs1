@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
java -classpath "%~dp0.mvn\wrapper\maven-wrapper.jar" "-Dmaven.multiModuleProjectDirectory=%~dp0." org.apache.maven.wrapper.MavenWrapperMain %*

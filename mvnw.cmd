@ECHO OFF
REM ---------------------------------------------------------------------------
REM Apache Maven Wrapper startup script for Windows (compatible with 3.3.x)
REM ---------------------------------------------------------------------------

SETLOCAL ENABLEDELAYEDEXPANSION
SET MVNW_VERBOSE=%MVNW_VERBOSE%

SET DIRNAME=%~dp0
IF "%DIRNAME%" == "" SET DIRNAME=.
SET BASEDIR=%DIRNAME%

SET WRAPPER_DIR=%BASEDIR%\.mvn\wrapper
SET WRAPPER_JAR=%WRAPPER_DIR%\maven-wrapper.jar
SET WRAPPER_PROPERTIES=%WRAPPER_DIR%\maven-wrapper.properties

IF EXIST "%WRAPPER_PROPERTIES%" (
  FOR /F "usebackq tokens=1,* delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
    IF /I "%%A"=="wrapperUrl" SET WRAPPER_URL=%%B
  )
)

IF "%JAVA_HOME%"=="" (
  SET JAVACMD=java
) ELSE (
  SET JAVACMD="%JAVA_HOME%\bin\java.exe"
)

REM Download wrapper JAR if missing
IF NOT EXIST "%WRAPPER_JAR%" (
  IF "%MVNW_VERBOSE%"=="true" ECHO Downloading Maven Wrapper JAR ...
  IF NOT EXIST "%WRAPPER_DIR%" MKDIR "%WRAPPER_DIR%"

  IF NOT DEFINED WRAPPER_URL (
    ECHO wrapperUrl not set in %WRAPPER_PROPERTIES%
    EXIT /B 1
  )

  REM Try powershell, curl, then wget
  powershell -NoProfile -NonInteractive -Command "[Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')" 2> NUL
  IF EXIST "%WRAPPER_JAR%" GOTO downloadDone

  WHERE curl >NUL 2>&1
  IF NOT ERRORLEVEL 1 (
    curl -fsSL -o "%WRAPPER_JAR%" "%WRAPPER_URL%"
    IF EXIST "%WRAPPER_JAR%" GOTO downloadDone
  )

  WHERE wget >NUL 2>&1
  IF NOT ERRORLEVEL 1 (
    wget -q -O "%WRAPPER_JAR%" "%WRAPPER_URL%"
    IF EXIST "%WRAPPER_JAR%" GOTO downloadDone
  )

  ECHO Failed to download %WRAPPER_URL%
  EXIT /B 1

:downloadDone
)

SET WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

IF "%MVNW_VERBOSE%"=="true" (
  ECHO Using BASEDIR: %BASEDIR%
  ECHO Using JAVA: %JAVACMD%
  ECHO Using WRAPPER_JAR: %WRAPPER_JAR%
)

"%JAVACMD%" %MAVEN_OPTS% -classpath "%WRAPPER_JAR%" %WRAPPER_LAUNCHER% %*

ENDLOCAL

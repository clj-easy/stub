@echo off

echo Building stub %STUB_JAR% with Xmx of %STUB_XMX%

rem the --no-server option is not supported in GraalVM Windows.
call %GRAALVM_HOME%\bin\native-image.cmd ^
      "-jar" "%STUB_JAR%" ^
      "-H:+ReportExceptionStackTraces" ^
      "--verbose" ^
      "--no-fallback" ^
      "--native-image-info" ^
      "%STUB_XMX%"

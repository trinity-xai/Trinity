@echo off

FOR /f "delims=" %%a IN ('dir /s /b ..\target\trinity-*-assembly.jar') DO SET "TRINITY_JAR=%%a"

java -jar %TRINITY_JAR% -Dprism.maxvram=2G -Djavafx.animation.fullspeed=true -Dprism.experimental.skipMeshNormalComputation=true --outrun=true --matrix=true

@echo off

FOR /f "delims=" %%a IN ('dir /s /b ..\target\trinity-*-assembly.jar') DO SET "TRINITY_JAR=%%a"

java -Dprism.maxvram=2G -cp %TRINITY_JAR% edu.jhuapl.trinity.utils.fun.DalleWalleMain --scanPath=../dalle

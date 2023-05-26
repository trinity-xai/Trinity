#!/usr/bin/env bash

"${JAVA_HOME}"/bin/java -Dprism.maxvram=2G -Djavafx.animation.fullspeed=true -jar ../target/trinity-1.0.0-SNAPSHOT-assembly.jar

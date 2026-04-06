@echo off
set MVN="C:\Program Files\OpenIDE\OpenIDE 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
%MVN% test -Dtest=HeuristicEvaluationTest -DfailIfNoTests=false -Dsurefire.useFile=false --no-transfer-progress

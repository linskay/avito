@echo off
set MVN="C:\Program Files\OpenIDE\OpenIDE 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd"
echo ============================================
echo  BUILDING PROJECT (SKIP TESTS)
echo ============================================
%MVN% compile -DskipTests --no-transfer-progress
echo ============================================
pause

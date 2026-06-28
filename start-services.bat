@echo off
echo ===================================================
echo   Initializing SmartCampus Infrastructure...
echo ===================================================

:: 1. Start Docker Containers
echo Starting Docker services (Database and ActiveMQ)...
docker compose up -d

:: 2. Wait for services to initialize
echo Waiting for infrastructure to initialize...
timeout /t 10

echo ===================================================
echo   Launching Microservices...
echo ===================================================

start "Profile Service (8081)" cmd /k "cd student-profile\target && java -jar student-profile-1.0.0.jar"
start "Enrolment Service (8082)" cmd /k "cd course-enrolment\target && java -jar course-enrolment-1.0.0.jar"
start "Library Service (8083)" cmd /k "cd library-booking\target && java -jar library-booking-1.0.0.jar"
start "Notification Service (8084)" cmd /k "cd notification\target && java -jar notification-1.0.0.jar"
start "Analytics Service (8085)" cmd /k "cd reporting-analytics\target && java -jar reporting-analytics-1.0.0.jar"

echo Services are launching.
pause
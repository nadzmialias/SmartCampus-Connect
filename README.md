# **SmartCampus Connect**

SmartCampus Connect is a fault-tolerant, event-driven microservices architecture designed to handle high-concurrency university workflows, such as semester course registration, student profile management, and legacy library system integration.

---

## 🏗️ Architecture Overview

The system moves away from a tightly coupled monolith to a distributed architecture using Java Spring Boot, MySQL/MariaDB (Database-per-Service pattern), and Apache ActiveMQ (Asynchronous Choreography).

**Core Microservices**
*   **Student Profile Service (Port 8081):** REST API for managing student demographics.
*   **Course Enrolment Service (Port 8082):** Highly concurrent REST API protected by `ReentrantLock`. Triggers asynchronous events.
*   **Library Booking Service (Port 8083):** Legacy SOAP service using Apache CXF.
*   **Notification Service (Port 8084):** Background consumer that listens to the `enrolmentQueue` to dispatch alerts and audit logs.
*   **Reporting/Analytics Service (Port 8085):** Aggregates cross-domain data via the Master-Slave pattern to generate administrative insights.

---

## 🚀 Quick Start Guide (Under 15 Minutes)

### Prerequisites
To run this project on a clean machine, ensure you have the following installed:
*   **Java 17 (JDK)**
*   **Apache Maven**
*   **MySQL/MariaDB** running on Port `3306` (via XAMPP or local install)
*   **Docker Desktop** (Must be running for the ActiveMQ broker)
*   **Postman** (For API testing)

### Step 1: Start the Infrastructure
This project relies on Docker to instantly provision the Apache ActiveMQ message broker.
Open your terminal at the root of the repository and run:
> `docker compose up -d`

*(Ensure your local XAMPP/MariaDB server is also started).*

### Step 2: Build the Application
We have provided a single-command build to compile all microservices into executable Spring Boot Fat JARs. Run the following command from the root directory:
> `mvn clean install`

### Step 3: Launch the Services (One-Click)
To facilitate rapid deployment, we have included a startup script that launches all five services simultaneously in their own isolated terminal windows.

**On Windows:**
Double-click the `start-services.bat` file in the root directory, OR run it from your terminal:
> `.\start-services.bat`

Wait approximately 15 seconds for the Spring Boot ASCII logo to appear in all 5 windows, indicating the campus environment is fully online.

*(Manual Fallback: You can also navigate into each folder's `target/` directory and run `java -jar [filename].jar`).*

---

## 🧪 Testing the Architecture

We have included a complete Postman collection to easily test all service endpoints without manual configuration.

**API Testing with Postman**
1. Open Postman and click **Import**.
2. Select the `SmartCampus.postman_collection.json` file located in the root directory.
3. In Postman, ensure the collection variables point to your local machine (e.g., `profileAppUrl` = `http://localhost`, `profileAppPort` = `8081`, etc.).
4. You can now run the pre-configured tests directly from the folders.

**Synchronous REST & Graceful Degradation**
Trigger the *Generate Snapshot Report* request in Service 4.
*   **Failure Handling Test:** Terminate the Profile Service window (Port 8081) and trigger the report again. The Analytics service will gracefully catch the connection failure and return a safe `503 Service Unavailable` response instead of crashing.

**SOAP Contract**
To verify the legacy SOAP integration, navigate to:
> `http://localhost:8083/services/booking?wsdl`

**Concurrency Load Testing**
A pre-configured JMeter test plan (`DAD_project_test.jmx`) is included in the root directory.
*   It is configured to simulate 50 concurrent students attempting to enrol in a course with a limited capacity of 5 seats at the exact same millisecond.
*   Running this test proves the efficacy of the `ReentrantLock` mechanism, resulting in exactly 5 successes (`200 OK`) and 45 safely rejected conflicts (`409 Conflict`).
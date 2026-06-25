# **SmartCampus Connect**

Course: BITP 3123 - Distributed Application Design

SmartCampus Connect is a fault-tolerant, event-driven microservices architecture designed to handle high-concurrency university workflows, such as semester course registration, student profile management, and legacy library system integration.

-----------------------------------------------------------------------------------------------------------

🏗️ Architecture Overview

The system moves away from a tightly coupled monolith to a distributed architecture using Java Spring Boot, MariaDB (Database-per-Service pattern), and Apache ActiveMQ (Asynchronous Choreography).

Core Microservices
- **Student Profile Service (Port 8081)**: REST API for managing student demographics.
- **Course Enrolment Service (Port 8082)**: Highly concurrent REST API protected by ReentrantLock. Triggers asynchronous events.
- **Library Booking Service (Port 8083)**: Legacy SOAP service using Apache CXF.
- **Notification Service (Port 8084**: Background consumer that listens to the enrolmentQueue to dispatch alerts.
- **Reporting/Analytics Service (Port 8085)**: Aggregates cross-domain data to generate administrative insights and reports.

-----------------------------------------------------------------------------------------------------------

🚀 Quick Start Guide (Under 15 Minutes)

Prerequisites
To run this project on a clean machine, ensure you have the following installed:
- Java 17 (JDK)
- Apache Maven
- Docker Desktop (Must be running before executing the setup)
- Postman (For API testing)

-----------------------------------------------------------------------------------------------------------

****Step 1**: Start the Infrastructure (Databases & Message Broker)**

This project relies on Docker to instantly provision the isolated MariaDB databases and the Apache ActiveMQ broker.

Open your terminal at the root of the repository and run:
> **docker-compose up -d**

****Step 2**: Build the Application**

We have provided a single-command build to compile all microservices cleanly. Run the following command from the root directory: 
> **mvn clean install**

**Step 3: Launch the Services**

Start the microservices using your IDE's run configurations, or open separate terminal windows and run the following commands in their respective directories:

- Terminal 1: Profile Service
> cd student-profile

> mvn spring-boot:run

- Terminal 2: Enrolment Service
> cd course-enrolment

> mvn spring-boot:run

- Terminal 3: Notification Service
> cd notification

> mvn spring-boot:run

- Terminal 4: Library Booking Service
> cd library-booking

> mvn spring-boot:run

- Terminal 5: Reporting Analytics
> cd reporting analytics

> mvn spring-boot:run

-----------------------------------------------------------------------------------------------------------

🧪 Testing the Architecture

We have included a complete Postman collection to easily test all service endpoints without manual configuration.

**API Testing with Postman**

1. Open Postman and click Import.
2. Select the SmartCampus.postman_collection.json file located in the root directory.
3. In Postman, set the collection variables (or your environment variables) to point to your local machine:
   - profileAppUrl / enrollmentAppUrl / libraryAppUrl / analyticsAppUrl = http://localhost
   - profileAppPort = 8081
   - enrollmentAppPort = 8082
   - libraryAppPort = 8083
   - analyticsAppPort = 8085
4. You can now run the pre-configured Positive and Negative tests directly from the folders.


**Synchronous REST & Graceful Degradation**

Using the Postman collection, trigger the Enrol Student request (Port 8082).
- Failure Handling Test: If you stop the Profile Service (Port 8081) and attempt to enrol, the system will seamlessly catch the ResourceAccessException and return a safe 503 Service Unavailable response.

**SOAP Contract (WSDL)**
To verify the legacy SOAP integration, navigate to:
> http://localhost:8083/services/booking?wsdl

**Concurrency Load Testing**
A pre-configured JMeter test plan _(DAD_project_test.jmx)_ is included in the root directory.
- It is configured to simulate 50 concurrent students attempting to enrol in a course with a limited capacity of 5 seats at the exact same millisecond.
- Running this test proves the efficacy of the ReentrantLock mechanism, resulting in exactly 5 successes (200 OK) and 45 safely rejected conflicts (409 Conflict).


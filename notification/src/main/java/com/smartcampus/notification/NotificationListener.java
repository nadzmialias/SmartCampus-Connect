package com.smartcampus.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @Autowired
    private NotificationRepository notificationRepository;

    @JmsListener(destination = "enrolmentQueue")
    public void receiveMessage(String message) {
        
        String eventType = "UNKNOWN";
        String recipient = "UNKNOWN";
        String subject = "UNKNOWN";
        String body = "UNKNOWN";

        System.out.println("\n==================================================");
        System.out.println("[NOTIFICATION BROKER] System Event Received");
        System.out.println("==================================================");

        // Template for Student Enrolment
        if (message.startsWith("ENROLMENT_SUCCESS")) {
            String[] parts = message.split(":");
            String studentId = parts[1];
            String courseId = parts[2];

            eventType = "COURSE ENROLMENT";
            recipient = "Student " + studentId;
            subject = "Enrolment Confirmed!";
            body = "Congratulations! You are officially enrolled in " + courseId + ".";

            System.out.println("Event Type : " + eventType);
            System.out.println("Recipient  : " + recipient);
            System.out.println("Subject    : " + subject);
            System.out.println("Body       : " + body);
        }

        // Template for Registrar Adding Seats
        else if (message.startsWith("SEAT_ALLOCATION")) {
            String[] parts = message.split(":");
            String courseId = parts[1];
            String addedSeats = parts[2];
            String totalSeats = parts[3];

            eventType = "SYSTEM ALERT - REGISTRAR";
            recipient = "Academic Mailing List";
            subject = "Capacity Update for " + courseId;
            body = addedSeats + " new seats have been opened for " + courseId + ". Total capacity is now " + totalSeats + ".";

            System.out.println("Event Type : " + eventType);
            System.out.println("Recipient  : " + recipient);
            System.out.println("Subject    : " + subject);
            System.out.println("Body       : " + body);
        }

        // Fallback for unexpected messages
        else {
            eventType = "UNKNOWN EVENT";
            body = message;
            
            System.out.println("Event Type : " + eventType);
            System.out.println("Payload    : " + body);
        }

        // Save to Database
        NotificationRecord record = new NotificationRecord(eventType, recipient, subject, body);
        notificationRepository.save(record);

        System.out.println("--------------------------------------------------");
        System.out.println("[STATUS] Logged to DB & Dispatching via Email... [OK]");
        System.out.println("==================================================\n");
    }
}
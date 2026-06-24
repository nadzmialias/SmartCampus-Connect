package com.smartcampus.notification;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @JmsListener(destination = "enrolmentQueue")
    public void receiveMessage(String message) {
        System.out.println("\n==================================================");
        System.out.println("🔔 [NOTIFICATION BROKER] System Event Received");
        System.out.println("==================================================");

        // Template for Student Enrolment
        if (message.startsWith("ENROLMENT_SUCCESS")) {
            String[] parts = message.split(":");
            String studentId = parts[1];
            String courseId = parts[2];

            System.out.println("Event Type : COURSE ENROLMENT");
            System.out.println("Recipient  : Student " + studentId);
            System.out.println("Subject    : Enrolment Confirmed!");
            System.out.println("Body       : Congratulations! You are officially enrolled in " + courseId + ".");
        }

        // Template for Registrar Adding Seats
        else if (message.startsWith("SEAT_ALLOCATION")) {
            String[] parts = message.split(":");
            String courseId = parts[1];
            String addedSeats = parts[2];
            String totalSeats = parts[3];

            System.out.println("Event Type : SYSTEM ALERT - REGISTRAR");
            System.out.println("Recipient  : Academic Mailing List");
            System.out.println("Subject    : Capacity Update for " + courseId);
            System.out.println("Body       : " + addedSeats + " new seats have been opened for " + courseId
                    + ". Total capacity is now " + totalSeats + ".");
        }

        // Fallback for unexpected messages
        else {
            System.out.println("Event Type : UNKNOWN EVENT");
            System.out.println("Payload    : " + message);
        }

        System.out.println("--------------------------------------------------");
        System.out.println("[STATUS] Dispatching via Email & SMS... [OK]");
        System.out.println("==================================================\n");
    }
}
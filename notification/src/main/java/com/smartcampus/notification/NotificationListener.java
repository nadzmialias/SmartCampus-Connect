package com.smartcampus.notification;

import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationListener {

    @JmsListener(destination = "enrolment-queue")
    public void receiveMessage(String message) {
        System.out.println("==================================================");
        System.out.println("[ALERT] New Notification Triggered!");
        System.out.println("Payload: " + message);
        System.out.println("Status: Dispatching Email & SMS...");
        System.out.println("==================================================");
    }
}

package com.smartcampus.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
public class NotificationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    private String eventType;
    private String recipient;
    private String subject;
    
    @Column(length = 1000)
    private String messageBody;

    public NotificationRecord() {}

    public NotificationRecord(String eventType, String recipient, String subject, String messageBody) {
        this.timestamp = LocalDateTime.now();
        this.eventType = eventType;
        this.recipient = recipient;
        this.subject = subject;
        this.messageBody = messageBody;
    }

    // Getters
    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getEventType() { return eventType; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getMessageBody() { return messageBody; }
}
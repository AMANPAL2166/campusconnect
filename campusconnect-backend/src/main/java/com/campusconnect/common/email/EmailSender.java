package com.campusconnect.common.email;

public interface EmailSender {
    void send(String to, String subject, String body);
}
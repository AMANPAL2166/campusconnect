package com.campusconnect.auth.service;

record VerificationEmailRequested(String email, String rawToken) {}
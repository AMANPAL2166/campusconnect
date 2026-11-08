package com.campusconnect.auth.web.dto;
import com.campusconnect.common.security.Role;
public record MeResponse(long id, Role role) {}
package com.campusconnect.common.security;

/** The authenticated caller. Built from the JWT, so no DB call per request. */
public record AuthenticatedUser(long id, Role role) {}
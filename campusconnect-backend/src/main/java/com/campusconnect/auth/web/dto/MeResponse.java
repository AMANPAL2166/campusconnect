package com.campusconnect.auth.web.dto;
import com.campusconnect.common.security.Role;
import com.campusconnect.profile.api.ProfileDetails;

/** Private: includes email. Only ever returned to the account owner. */
public record MeResponse(long id, String email, boolean emailVerified, Role role, ProfileDetails profile) {

}
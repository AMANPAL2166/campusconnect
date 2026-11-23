package com.campusconnect.auth.web;

import com.campusconnect.auth.service.AccountService;
import com.campusconnect.auth.web.dto.MeResponse;
import com.campusconnect.common.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UserMeController {

    private final AccountService accounts;

    public UserMeController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal AuthenticatedUser me) {
        return accounts.me(me.id());
    }
}
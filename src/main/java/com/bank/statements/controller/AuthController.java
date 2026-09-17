package com.bank.statements.controller;

import com.bank.statements.dto.LoginRequest;
import com.bank.statements.dto.LoginResponse;
import com.bank.statements.security.CustomUserDetailsService;
import com.bank.statements.security.JwtService;
import com.bank.statements.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        // Throws BadCredentialsException (-> 401 via GlobalExceptionHandler) if invalid
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(principal);

        return new LoginResponse(token, principal.getId(), principal.getUser().getName(), principal.getUser().getRoles());
    }
}

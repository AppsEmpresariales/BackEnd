package com.eam.proyecto.presentationLayer.controller;

import com.eam.proyecto.businessLayer.service.auth.AuthService;
import com.eam.proyecto.securityLayer.dto.AuthRequests.LoginRequest;
import com.eam.proyecto.securityLayer.dto.AuthResponses.JwtResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {
        "http://localhost:4200",
        "http://127.0.0.1:4200",
        "http://localhost:4201",
        "http://127.0.0.1:4201"
})
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(@RequestBody com.eam.proyecto.securityLayer.dto.AuthRequests.RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
}

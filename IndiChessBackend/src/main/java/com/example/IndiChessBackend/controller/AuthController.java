package com.example.IndiChessBackend.controller;

import com.example.IndiChessBackend.model.DTO.LoginDto;
import com.example.IndiChessBackend.model.DTO.LoginResponseDto;
import com.example.IndiChessBackend.model.User;
import com.example.IndiChessBackend.service.AuthService;
import com.example.IndiChessBackend.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@CrossOrigin(value = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authservice;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;


    @PostMapping("signup")
    public ResponseEntity<String> handleSignup(@RequestBody User user) {
        authservice.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body("Signup successful. Please log in.");
    }

    @PostMapping("login")
    public ResponseEntity<?> handleLogin(HttpServletRequest request,
                                                        HttpServletResponse response,
                                                        @RequestBody LoginDto loginDto) throws IOException {


        Authentication authObject = authenticationManager.
                authenticate(new
                        UsernamePasswordAuthenticationToken
                        (loginDto.getUsername(), loginDto.getPassword()));
        if(authObject.isAuthenticated()) {
            String tk = jwtService.generateToken(loginDto.getUsername());
            System.out.println("Inside Auth controller");
            System.out.println(tk);



            ResponseCookie cookie = ResponseCookie.from("JWT", tk).httpOnly(true).
                    secure(false).sameSite("lax").path("/").maxAge(3600).build();
            response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());



            return ResponseEntity.ok(tk);
        }

        return new ResponseEntity<>(new LoginResponseDto(null, "Auth Failed"), HttpStatus.BAD_REQUEST);
    }

    @GetMapping("home")
    public ResponseEntity<?> handleHome(){
        System.out.println("Home");
        return ResponseEntity.ok("Home");
    }

    // New endpoint: return current authenticated username
    @GetMapping("me")
    public ResponseEntity<?> getCurrentUser() {
        if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
                !(SecurityContextHolder.getContext().getAuthentication() instanceof org.springframework.security.authentication.AnonymousAuthenticationToken)) {
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            return ResponseEntity.ok(username);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authenticated");
    }

    // Logout endpoint: clears security context and expires the JWT cookie
    @PostMapping("logout")
    public ResponseEntity<?> handleLogout(HttpServletRequest request, HttpServletResponse response) {
        // Clear server-side security context
        SecurityContextHolder.clearContext();

        // Expire the JWT cookie by setting maxAge to 0 for both secure and non-secure variants
        ResponseCookie cookieNonSecure = ResponseCookie.from("JWT", "").httpOnly(true)
                .secure(false).sameSite("lax").path("/").maxAge(0).build();
        ResponseCookie cookieSecure = ResponseCookie.from("JWT", "").httpOnly(true)
                .secure(true).sameSite("lax").path("/").maxAge(0).build();

        // Add both headers so the browser clears either cookie variant
        response.addHeader(HttpHeaders.SET_COOKIE, cookieNonSecure.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieSecure.toString());

        // Redirect to login page
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, "/login").build();
    }



}

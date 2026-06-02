package com.nalaka.goviya.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nalaka.goviya.model.User;
import com.nalaka.goviya.model.dto.ApiResponse;
import com.nalaka.goviya.model.dto.EmailSignupRequest;
import com.nalaka.goviya.model.dto.ForgotPasswordRequest;
import com.nalaka.goviya.model.dto.LoginRequest;
import com.nalaka.goviya.model.dto.LoginResponse;
import com.nalaka.goviya.model.dto.RegisterUserRequest;
import com.nalaka.goviya.model.dto.ResetPasswordRequest;
import com.nalaka.goviya.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
@Validated
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Register user (legacy endpoint - without OTP)
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user){
        try {
            User saved = userService.register(user);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Simple signup with email, password, full name, and role
     * POST /api/auth/signup
     * Body: {
     *   "fullName": "John Doe",
     *   "email": "john@example.com",
     *   "password": "password123",
     *   "role": "farmer",
     *   "phone": "0771234567" (optional),
     *   "province": "Western" (optional),
     *   "district": "Colombo" (optional),
     *   "city": "Colombo" (optional),
     *   "harvestTypes": ["Rice", "Vegetables"] (optional),
     *   "harvestArea": 2.5 (optional)
     * }
     * Valid roles: farmer, buyer, admin
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<User>> signupWithEmail(@Valid @RequestBody EmailSignupRequest request) {
        try {
            User savedUser = userService.registerWithEmail(request);
            
            // Don't send password back in response
            savedUser.setPassword(null);
            
            ApiResponse<User> response = new ApiResponse<>(
                    true,
                    "Account created successfully",
                    savedUser
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            log.error("Signup failed: {}", e.getMessage());
            ApiResponse<User> response = new ApiResponse<>(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error during signup: {}", e.getMessage(), e);
            ApiResponse<User> response = new ApiResponse<>(
                    false,
                    "An unexpected error occurred. Please try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Login with email/password or phone/OTP
     * POST /api/auth/login
     * Body: { "email": "john@example.com", "password": "password123" }
     * or:   { "phoneNumber": "0771234567", "otp": "123456" }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse loginResponse = userService.login(request);
            
            ApiResponse<LoginResponse> response = new ApiResponse<>(
                    true,
                    "Login successful",
                    loginResponse
            );
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Login failed: {}", e.getMessage());
            ApiResponse<LoginResponse> response = new ApiResponse<>(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error during login: {}", e.getMessage(), e);
            ApiResponse<LoginResponse> response = new ApiResponse<>(
                    false,
                    "An unexpected error occurred. Please try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Register user with OTP verification
     * POST /api/auth/register-with-otp
     * Body: {
     *   "firstName": "John",
     *   "lastName": "Doe",
     *   "email": "john@example.com",
     *   "password": "password123",
     *   "province": "Western",
     *   "district": "Colombo",
     *   "city": "Colombo",
     *   "phone": "0771234567",
     *   "optionalPhone": "0112345678",
     *   "harvestTypes": ["Rice", "Vegetables"],
     *   "harvestArea": 2.5,
     *   "otp": "123456"
     * }
     */
    @PostMapping("/register-with-otp")
    public ResponseEntity<ApiResponse<User>> registerUserWithOtp(@Valid @RequestBody RegisterUserRequest request) {
        try {
            User savedUser = userService.registerWithOtp(request);
            
            // Don't send password back in response
            savedUser.setPassword(null);
            
            ApiResponse<User> response = new ApiResponse<>(
                    true,
                    "User registered successfully",
                    savedUser
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            ApiResponse<User> response = new ApiResponse<>(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error during registration: {}", e.getMessage(), e);
            ApiResponse<User> response = new ApiResponse<>(
                    false,
                    "An unexpected error occurred. Please try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Forgot password - sends OTP to user's email
     * POST /api/auth/forgot-password
     * Body: {
     *   "email": "john@example.com"
     * }
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            Map<String, Object> result = userService.forgotPassword(request.getEmail());
            
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    true,
                    "OTP sent to your email successfully",
                    result
            );
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Forgot password failed: {}", e.getMessage());
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error during forgot password: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    false,
                    "An unexpected error occurred. Please try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Reset password with OTP
     * POST /api/auth/reset-password
     * Body: {
     *   "email": "john@example.com",
     *   "otp": "123456",
     *   "newPassword": "newPassword123"
     * }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            Map<String, Object> result = userService.resetPassword(
                    request.getEmail(), 
                    request.getOtp(), 
                    request.getNewPassword()
            );
            
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    true,
                    "Password reset successfully",
                    result
            );
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            log.error("Reset password failed: {}", e.getMessage());
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(false, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            
        } catch (Exception e) {
            log.error("Unexpected error during reset password: {}", e.getMessage(), e);
            ApiResponse<Map<String, Object>> response = new ApiResponse<>(
                    false,
                    "An unexpected error occurred. Please try again."
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get user by ID
     * GET /api/auth/{userId}
     */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable String userId) {
        return userService.getUserById(userId)
            .map(user -> {
                user.setPassword(null); // Don't expose password
                return ResponseEntity.ok(new ApiResponse<>(true, "User retrieved successfully", user));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiResponse<>(false, "User not found", null)));
    }
}

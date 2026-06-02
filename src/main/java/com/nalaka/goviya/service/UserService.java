package com.nalaka.goviya.service;


import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.nalaka.goviya.model.User;
import com.nalaka.goviya.model.dto.EmailSignupRequest;
import com.nalaka.goviya.model.dto.LoginRequest;
import com.nalaka.goviya.model.dto.LoginResponse;
import com.nalaka.goviya.model.dto.RegisterUserRequest;
import com.nalaka.goviya.repository.UserRepository;
import com.nalaka.goviya.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepository repo;

    @Autowired
    private OtpService otpService;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private JwtUtil jwtUtil;

    public User register(User user) {
        if(repo.existsByEmail(user.getEmail())){
            throw new RuntimeException("User already exists");
        }

        //Need to encrypt password before saving

        return repo.save(user);
    }

    /**
     * Register user with email and password (simple signup)
     */
    public User registerWithEmail(EmailSignupRequest request) {
        // Validate role
        if (request.getRole() == null) {
            throw new RuntimeException("Role is required");
        }
        String role = request.getRole().toLowerCase();
        if (!role.equals("farmer") && !role.equals("buyer") && !role.equals("admin")) {
            throw new RuntimeException("Invalid role. Must be 'farmer', 'buyer', or 'admin'");
        }

        // This endpoint requires an email. For mobile-only signup use the OTP flow (/api/auth/register-with-otp)
        if (!hasText(request.getEmail())) {
            throw new RuntimeException("Email is required for this signup endpoint. For mobile-only signup use the OTP flow: send OTP to phone and call /api/auth/register-with-otp");
        }

        // Email signup flow
        if (repo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("User already exists with this email");
        }

        User user = new User();
        user.setFullName(request.getFullName());

        // Parse fullName into firstName and lastName for backward compatibility
        String[] nameParts = request.getFullName() != null ? request.getFullName().trim().split(" ", 2) : new String[]{"", ""};
        user.setFirstName(nameParts.length > 0 ? nameParts[0] : "");
        user.setLastName(nameParts.length > 1 ? nameParts[1] : "");

        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword())); // Encrypt password
        user.setRole(role);
        user.setEmailVerified(false);

        // Set optional fields if provided
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            user.setPhone(request.getPhone());
        }
        if (request.getProvince() != null && !request.getProvince().isEmpty()) {
            user.setProvice(request.getProvince());
        }
        if (request.getDistrict() != null && !request.getDistrict().isEmpty()) {
            user.setDistrict(request.getDistrict());
        }
        if (request.getCity() != null && !request.getCity().isEmpty()) {
            user.setCity(request.getCity());
        }
        if (request.getHarvestTypes() != null && request.getHarvestTypes().length > 0) {
            user.setHarvestTypes(request.getHarvestTypes());
        }
        if (request.getHarvestArea() != null && request.getHarvestArea() > 0) {
            user.setHarvestArea(request.getHarvestArea());
        }

        User savedUser = repo.save(user);
        log.info("User registered successfully with email: {} and role: {}", savedUser.getEmail(), savedUser.getRole());

        return savedUser;
    }

    /**
     * Login with email/password or phone/OTP
     */
    public LoginResponse login(LoginRequest request) {
        if (hasText(request.getPhoneNumber()) || hasText(request.getOtp())) {
            if (!hasText(request.getPhoneNumber()) || !hasText(request.getOtp())) {
                throw new RuntimeException("Phone number and OTP are required for mobile login");
            }

            return loginWithPhoneOtp(request.getPhoneNumber(), request.getOtp());
        }

        if (hasText(request.getEmail()) || hasText(request.getPassword())) {
            if (!hasText(request.getEmail()) || !hasText(request.getPassword())) {
                throw new RuntimeException("Email and password are required for email login");
            }

            return loginWithEmailPassword(request.getEmail(), request.getPassword());
        }

        throw new RuntimeException("Email/password or phone/OTP is required for login");
    }

    private LoginResponse loginWithEmailPassword(String email, String password) {
        Optional<User> userOpt = repo.findByEmail(email);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = userOpt.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return buildLoginResponse(user, user.getEmail());
    }

    private LoginResponse loginWithPhoneOtp(String phoneNumber, String otp) {
        Map<String, Object> otpResult = otpService.verifyOtp(phoneNumber, otp, "login");

        boolean otpVerified = (boolean) otpResult.get("success");
        if (!otpVerified) {
            throw new RuntimeException((String) otpResult.get("message"));
        }

        Optional<User> userOpt = findUserByPhoneNumber(phoneNumber);
        if (userOpt.isEmpty()) {
            // No account found. Convert login OTP to signup OTP so user can register
            otpService.changePhoneOtpPurposeLoginToSignup(phoneNumber, otp);
            throw new RuntimeException("No account found with this mobile number. Please register using the signup endpoint.");
        }

        User user = userOpt.get();
        String tokenSubject = StringUtils.hasText(user.getEmail()) ? user.getEmail() : normalizePhoneNumber(phoneNumber);
        return buildLoginResponse(user, tokenSubject);
    }

    private LoginResponse buildLoginResponse(User user, String tokenSubject) {
        log.info("User logged in successfully: {}", StringUtils.hasText(user.getEmail()) ? user.getEmail() : user.getPhone());

        String token = jwtUtil.generateToken(user.getId(), tokenSubject, user.getRole());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setPhone(user.getPhone());
        response.setProvince(user.getProvice());
        response.setDistrict(user.getDistrict());
        response.setCity(user.getCity());
        response.setHarvestTypes(user.getHarvestTypes());
        response.setHarvestArea(user.getHarvestArea());

        return response;
    }

    private Optional<User> findUserByPhoneNumber(String phoneNumber) {
        Set<String> candidates = new LinkedHashSet<>();
        candidates.add(phoneNumber);
        candidates.add(normalizePhoneNumber(phoneNumber));

        for (String candidate : candidates) {
            if (!StringUtils.hasText(candidate)) {
                continue;
            }

            Optional<User> userByPhone = repo.findByPhone(candidate);
            if (userByPhone.isPresent()) {
                return userByPhone;
            }

            Optional<User> userByOptionalPhone = repo.findByOptionalPhone(candidate);
            if (userByOptionalPhone.isPresent()) {
                return userByOptionalPhone;
            }
        }

        return Optional.empty();
    }

    private String normalizePhoneNumber(String phoneNumber) {
        if (!StringUtils.hasText(phoneNumber)) {
            return phoneNumber;
        }

        String trimmedPhone = phoneNumber.trim();
        if (trimmedPhone.startsWith("+94")) {
            return trimmedPhone;
        }

        if (trimmedPhone.startsWith("0")) {
            return "+94" + trimmedPhone.substring(1);
        }

        return "+94" + trimmedPhone;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    /**
     * Register user with OTP verification
     */
    public User registerWithOtp(RegisterUserRequest request) {
        // Verify OTP first
        Map<String, Object> otpResult = otpService.verifyOtp(
                request.getPhone(), 
                request.getOtp(), 
                "signup"
        );

        boolean otpVerified = (boolean) otpResult.get("success");
        if (!otpVerified) {
            throw new RuntimeException("OTP verification failed: " + otpResult.get("message"));
        }

        // Check if user already exists (email or phone depending on provided data)
        if (hasText(request.getEmail())) {
            if (repo.existsByEmail(request.getEmail())) {
                // If email already exists, return existing user after OTP verification
                return repo.findByEmail(request.getEmail()).get();
            }
        }

        // If phone already exists, return the existing user after OTP verification
        Optional<User> existingByPhone = findUserByPhoneNumber(request.getPhone());
        if (existingByPhone.isPresent()) {
            return existingByPhone.get();
        }

        // If there is no existing account for this phone, keep the OTP reusable for the next registration attempt
        otpService.markPhoneOtpUnused(request.getPhone(), request.getOtp(), "signup");

        // Create user object
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        if (hasText(request.getEmail())) {
            user.setEmail(request.getEmail());
        }

        if (hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword())); // Encrypt password
        }

        user.setProvice(request.getProvince());
        user.setDistrict(request.getDistrict());
        user.setCity(request.getCity());
        user.setPhone(request.getPhone());
        user.setOptionalPhone(request.getOptionalPhone());
        user.setHarvestTypes(request.getHarvestTypes());
        
        if (request.getHarvestArea() != null && request.getHarvestArea() > 0) {
            user.setHarvestArea(request.getHarvestArea());
        }
        
        user.setEmailVerified(false);

        User savedUser = repo.save(user);
        log.info("User registered successfully: {}", hasText(savedUser.getEmail()) ? savedUser.getEmail() : savedUser.getPhone());

        return savedUser;
    }

    /**
     * Initiate forgot password - sends OTP to user's email
     */
    public Map<String, Object> forgotPassword(String email) {
        // Check if user exists
        Optional<User> userOpt = repo.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("No account found with this email");
        }

        User user = userOpt.get();
        String userName = user.getFullName() != null ? user.getFullName() : user.getFirstName();

        // Send OTP to email
        Map<String, Object> result = otpService.sendEmailOtp(email, userName, "password-reset");
        
        if (!(boolean) result.get("success")) {
            throw new RuntimeException("Failed to send OTP: " + result.get("message"));
        }

        log.info("Password reset OTP sent to: {}", email);
        return result;
    }

    /**
     * Reset password with OTP verification
     */
    public Map<String, Object> resetPassword(String email, String otp, String newPassword) {
        // Verify OTP
        Map<String, Object> otpResult = otpService.verifyEmailOtp(email, otp, "password-reset");

        boolean otpVerified = (boolean) otpResult.get("success");
        if (!otpVerified) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        // Find user by email
        Optional<User> userOpt = repo.findByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User user = userOpt.get();
        
        // Update password
        user.setPassword(passwordEncoder.encode(newPassword));
        repo.save(user);

        log.info("Password reset successfully for user: {}", email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successfully");
        
        return response;
    }
    public Optional<User> getUserById(String userId) {
        return repo.findById(userId);
    }
}

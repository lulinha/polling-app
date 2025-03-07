package com.example.polls.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.polls.exception.AppException;
import com.example.polls.exception.BadRequestException;
import com.example.polls.model.Role;
import com.example.polls.model.RoleName;
import com.example.polls.model.User;
import com.example.polls.payload.JwtAuthenticationResponse;
import com.example.polls.payload.LoginRequest;
import com.example.polls.payload.SignUpRequest;
import com.example.polls.payload.SignUpResponse;
import com.example.polls.repository.RoleRepository;
import com.example.polls.repository.UserRepository;
import com.example.polls.security.JwtTokenProvider;
import com.example.polls.util.JsonUtils;

@Service
public class AuthService {

        @Autowired
        private AuthenticationManager authenticationManager;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Autowired
        private JwtTokenProvider tokenProvider;

        @Autowired
        private KafkaProducerService kafkaProducerService;

        public JwtAuthenticationResponse authenticateUser(LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getUsernameOrEmail(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = tokenProvider.generateToken(authentication);
                return new JwtAuthenticationResponse(jwt);
        }

        public SignUpResponse registerUser(SignUpRequest signUpRequest) {
                if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                        throw new BadRequestException("Username is already taken!");
                }
                if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                        throw new BadRequestException("Email Address already in use!");
                }

                User user = new User(signUpRequest.getName(), signUpRequest.getUsername(),
                                signUpRequest.getEmail(), signUpRequest.getPassword());

                user.setPassword(passwordEncoder.encode(user.getPassword()));

                Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                .orElseThrow(() -> new AppException("User Role not set."));

                user.setRoles(Collections.singleton(userRole));

                User savedUser = userRepository.save(user);

                SignUpResponse response = new SignUpResponse(savedUser.getId(), savedUser.getUsername(),
                                savedUser.getEmail());

                // 将 DTO 转换为 JSON
                String message = JsonUtils.toJson(response);

                kafkaProducerService.sendWithPersistence("user-registration", savedUser.getId().toString(), message);

                return response;
        }
}
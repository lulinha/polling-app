package com.example.polls.service;

import java.net.URI;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.polls.dto.UserDTO;
import com.example.polls.exception.AppException;
import com.example.polls.model.Role;
import com.example.polls.model.RoleName;
import com.example.polls.model.User;
import com.example.polls.payload.ApiResponse;
import com.example.polls.payload.JwtAuthenticationResponse;
import com.example.polls.payload.LoginRequest;
import com.example.polls.payload.SignUpRequest;
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

        public ResponseEntity<?> authenticateUser(LoginRequest loginRequest) {
                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                loginRequest.getUsernameOrEmail(),
                                                loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = tokenProvider.generateToken(authentication);
                return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
        }

        public ResponseEntity<?> registerUser(SignUpRequest signUpRequest) {
                if (userRepository.existsByUsername(signUpRequest.getUsername())) {
                        return new ResponseEntity<>(new ApiResponse(false, "Username is already taken!"),
                                        HttpStatus.BAD_REQUEST);
                }

                if (userRepository.existsByEmail(signUpRequest.getEmail())) {
                        return new ResponseEntity<>(new ApiResponse(false, "Email Address already in use!"),
                                        HttpStatus.BAD_REQUEST);
                }

                // Creating user's account
                User user = new User(signUpRequest.getName(), signUpRequest.getUsername(),
                                signUpRequest.getEmail(), signUpRequest.getPassword());

                user.setPassword(passwordEncoder.encode(user.getPassword()));

                Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                                .orElseThrow(() -> new AppException("User Role not set."));

                user.setRoles(Collections.singleton(userRole));

                User savedUser = userRepository.save(user);

                // 发送用户注册事件
                // kafkaProducerService.sendUserRegistrationEvent(savedUser.getId());

                // 将用户信息转换为 DTO
                UserDTO userDTO = new UserDTO();
                userDTO.setId(savedUser.getId());
                userDTO.setUsername(savedUser.getUsername());
                userDTO.setEmail(savedUser.getEmail());

                // 将 DTO 转换为 JSON
                String message = JsonUtils.toJson(userDTO);

                kafkaProducerService.sendWithPersistence("user-registration", savedUser.getId().toString(), message);

                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath().path("/users/{username}")
                                .buildAndExpand(savedUser.getUsername()).toUri();

                return ResponseEntity.created(location).body(new ApiResponse(true, "User registered successfully"));
        }
}
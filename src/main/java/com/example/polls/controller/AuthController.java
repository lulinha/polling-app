package com.example.polls.controller;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.polls.payload.ApiResponse;
import com.example.polls.payload.JwtAuthenticationResponse;
import com.example.polls.payload.LoginRequest;
import com.example.polls.payload.SignUpRequest;
import com.example.polls.payload.SignUpResponse;
import com.example.polls.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

        @Autowired
        private AuthService authService;

        @PostMapping("/signin")
        public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
                JwtAuthenticationResponse response = authService.authenticateUser(loginRequest);
                return ResponseEntity.ok(response);
        }

        @PostMapping("/signup")
        public ResponseEntity<ApiResponse<SignUpResponse>> registerUser(
                        @Valid @RequestBody SignUpRequest signUpRequest) {
                SignUpResponse response = authService.registerUser(signUpRequest);

                // 构建 Location Header, 遵循 RESTful API 设计规范，明确告知客户端新创建资源的访问路径
                // 注册成功后，客户端可通过 Location: http://localhost:8080/users/johndoe 直接访问新用户的详细信息。
                URI location = ServletUriComponentsBuilder
                                .fromCurrentContextPath()
                                .path("/users/{username}")
                                .buildAndExpand(response.getUsername())
                                .toUri();
                // 返回响应（可自定义 Body 内容）
                ApiResponse<SignUpResponse> apiResponse = new ApiResponse<>(true, "User registered successfully",
                                response);
                return ResponseEntity
                                .created(location)
                                .body(apiResponse);
        }
}
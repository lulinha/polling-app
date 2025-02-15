package com.example.polls.exception;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理业务逻辑异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        logger.error("BusinessException: {}", ex.getMessage(), ex);

        ErrorResponse error = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getErrorCode(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理参数校验异常（@Valid失败）
     */
    /* */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        
        logger.error("Validation error: {}", ex.getMessage(), ex);

        Map<String, String> errors = ex.getBindingResult().getAllErrors().stream()
                .collect(Collectors.toMap(
                        error -> ((FieldError) error).getField(),
                        error -> error.getDefaultMessage()
                ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "参数校验失败",
                null,
                errors
        );
    }

    /**
     * 处理 BadRequestException 异常
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadRequestException.class)
    public ErrorResponse handleBadRequestException(
            BadRequestException ex, HttpServletRequest request) {
        
        logger.error("BadRequestException: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "BAD_REQUEST",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 处理资源未找到异常
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ErrorResponse handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        logger.error("ResourceNotFoundException: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 处理权限不足异常
     */
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ErrorResponse handleAccessDeniedException(
            AccessDeniedException ex) {
        
        logger.error("AccessDeniedException: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "PERMISSION_DENIED",
                ex.getMessage(),
                null
        );
    }

    /**
     * 处理 AppException 异常
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(AppException.class)
    public ErrorResponse handleAppException(
            AppException ex, HttpServletRequest request) {
        
        logger.error("AppException: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "APP_EXCEPTION",
                ex.getMessage(),
                request.getRequestURI()
        );
    }

    /**
     * 兜底异常处理，用于捕获所有未被其他 @ExceptionHandler 方法处理的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(Exception ex, HttpServletRequest request) {
        
        logger.error("Unexpected exception: {}", ex.getMessage(), ex);

        String message = isProduction() ? "服务器内部错误" : ex.getMessage();

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                message,
                request.getRequestURI()
        );
    }

    /**
     * 构建错误响应
     */
    private ErrorResponse buildErrorResponse(
            HttpStatus status, String code, String message, String path) {
        return buildErrorResponse(status, code, message, path, null);
    }

    private ErrorResponse buildErrorResponse(
            HttpStatus status, String code, String message, String path, Object details) {
        return new ErrorResponse.Builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .code(code)
                .message(message)
                .path(path)
                .details(details)
                .build();
    }

    /**
     * 判断是否为生产环境
     */
    private boolean isProduction() {
        // 根据实际环境配置判断
        return "prod".equals(System.getProperty("env"));
    }

}
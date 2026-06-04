package com.kep.shared.web;

import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity
            .status(ex.code().status())
            .body(ApiResponse.error(ex.code().name(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity
            .status(ErrorCode.INTERNAL.status())
            .body(ApiResponse.error(ErrorCode.INTERNAL.name(), "服务器内部错误"));
    }
}

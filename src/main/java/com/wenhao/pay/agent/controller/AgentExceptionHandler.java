package com.wenhao.pay.agent.controller;

import com.wenhao.pay.agent.advisor.UnsafeRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * 全局异常处理：安全护栏拦截的请求统一返回 403。
 */
@RestControllerAdvice
public class AgentExceptionHandler {

    @ExceptionHandler(UnsafeRequestException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Map<String, String> handleUnsafe(UnsafeRequestException e) {
        return Map.of("error", e.getMessage());
    }
}

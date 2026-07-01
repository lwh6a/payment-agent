package com.wenhao.pay.agent.advisor;

/**
 * 安全护栏拦截到危险意图时抛出。由全局异常处理器转为 403。
 */
public class UnsafeRequestException extends RuntimeException {

    public UnsafeRequestException(String message) {
        super(message);
    }
}

package com.wenhao.pay.agent.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Tool 调用日志切面：记录每一次 @Tool 工具方法的调用入参、耗时、结果与异常。
 *
 * 切点是所有标注了 Spring AI {@code @Tool} 的方法，因此新增工具无需改切面，自动生效。
 * （前提：Tool Bean 走 Spring 代理，方法调用经过代理对象）
 */
@Aspect
@Component
public class ToolCallLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ToolCallLogAspect.class);

    /** 单条日志中结果文本的最大长度，避免刷屏。 */
    private static final int MAX_RESULT_LEN = 500;

    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object logToolCall(ProceedingJoinPoint pjp) throws Throwable {
        String tool = pjp.getSignature().getDeclaringType().getSimpleName()
                + "#" + pjp.getSignature().getName();
        long start = System.currentTimeMillis();
        log.info("[Tool-调用] {} 入参={}", tool, Arrays.toString(pjp.getArgs()));
        try {
            Object result = pjp.proceed();
            log.info("[Tool-完成] {} 耗时={}ms 结果={}", tool, System.currentTimeMillis() - start, brief(result));
            return result;
        } catch (Throwable e) {
            log.error("[Tool-异常] {} 耗时={}ms 错误={}", tool, System.currentTimeMillis() - start, e.getMessage());
            throw e;
        }
    }

    private String brief(Object result) {
        if (result == null) {
            return "null";
        }
        String text = result.toString();
        return text.length() > MAX_RESULT_LEN ? text.substring(0, MAX_RESULT_LEN) + "...(truncated)" : text;
    }
}

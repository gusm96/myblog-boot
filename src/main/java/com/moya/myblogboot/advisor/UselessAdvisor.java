package com.moya.myblogboot.advisor;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Slf4j
@Aspect
@Component
public class UselessAdvisor {

    @Around("execution(public * com.moya.myblogboot.service.implementation.*.*(..))")
    public Object stopWatch(ProceedingJoinPoint joinPoint) throws Throwable {
        StopWatch stopWatch = new StopWatch();
        try{
            stopWatch.start();
            return joinPoint.proceed();
        }finally {
            stopWatch.stop();
            log.info("request spent {} ms", stopWatch.getLastTaskTimeMillis());
        }
    }
}

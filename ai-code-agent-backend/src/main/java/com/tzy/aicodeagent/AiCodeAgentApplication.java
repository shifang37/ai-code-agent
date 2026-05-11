package com.tzy.aicodeagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan("com.tzy.aicodeagent.mapper")
public class AiCodeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeAgentApplication.class, args);
    }

}

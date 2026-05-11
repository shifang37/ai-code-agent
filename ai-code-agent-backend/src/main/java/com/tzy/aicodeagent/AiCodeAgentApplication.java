package com.tzy.aicodeagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)

public class AiCodeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeAgentApplication.class, args);
    }

}

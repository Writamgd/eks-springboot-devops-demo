package com.demo.eksdemo.controller;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    private final Counter requestCounter;

    public HelloController(MeterRegistry registry) {
        this.requestCounter = Counter.builder("demo_requests_total")
                .description("Total number of requests to home endpoint")
                .register(registry);
    }

    @GetMapping("/")
    public String hello() {
        requestCounter.increment();
        return "Hello from EKS 🚀 DevOps Learning Project";
    }
}
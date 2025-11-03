package com.demo.validation;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
public class ValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationApplication.class, args);
    }

    @Component
    @Order(1)
    public static class CorrelationIdFilter implements Filter {
        
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String correlationId = httpRequest.getHeader("correlation-id");
            
            if (correlationId == null || correlationId.isEmpty()) {
                correlationId = "gen-" + System.currentTimeMillis();
            }
            
            MDC.put("correlation_id", correlationId);
            MDC.put("method", httpRequest.getMethod());
            MDC.put("path", httpRequest.getRequestURI());
            
            try {
                chain.doFilter(request, response);
            } finally {
                MDC.clear();
            }
        }
    }
}

@RestController
class ValidationController {
    
    private static final Logger log = LoggerFactory.getLogger(ValidationController.class);
    private static final Random random = new Random();

    @GetMapping("/s3")
    public ResponseEntity<Map<String, Object>> s3Operation() {
        // Dummy S3 validation
        String bucketName = "my-bucket-" + random.nextInt(100);
        boolean bucketExists = random.nextFloat() > 0.2; // 80% success rate

        HttpStatus status = bucketExists ? HttpStatus.OK : HttpStatus.FAILED_DEPENDENCY;
        String message = bucketExists 
            ? "S3 operation successful: bucket accessible" 
            : "S3 operation failed: bucket dependency unavailable";

        MDC.put("endpoint", "s3");
        
        if (status == HttpStatus.FAILED_DEPENDENCY) {
            log.error(message);
        } else {
            log.info(message);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("bucket", bucketName);
        response.put("status", status.value());
        response.put("message", message);

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/rds")
    public ResponseEntity<Map<String, Object>> rdsOperation() {
        // Dummy RDS validation
        int queryResult = random.nextInt(100);
        boolean dbAvailable = queryResult > 20; // 80% success rate

        HttpStatus status = dbAvailable ? HttpStatus.OK : HttpStatus.FAILED_DEPENDENCY;
        String message = dbAvailable 
            ? "RDS operation successful: database query completed" 
            : "RDS operation failed: database dependency unavailable";

        MDC.put("endpoint", "rds");
        
        if (status == HttpStatus.FAILED_DEPENDENCY) {
            log.error(message);
        } else {
            log.info(message);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("query_result", queryResult);
        response.put("status", status.value());
        response.put("message", message);

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy");
    }
}
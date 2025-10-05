package com.example.agent.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for production-ready AI agents.
 * 
 * Implements enterprise patterns:
 * - Circuit Breaker: Prevents cascading failures
 * - Retry: Handles transient errors
 * - Rate Limiter: Protects backend services
 * - Time Limiter: Prevents hanging requests
 * - Bulkhead: Isolates failures
 * 
 * These patterns are essential when calling external LLM APIs
 * or vector databases that may be slow or temporarily unavailable.
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * Circuit Breaker for LLM calls.
     * 
     * Opens circuit when failure rate exceeds threshold,
     * preventing further calls and giving the service time to recover.
     * 
     * States:
     * - CLOSED: Normal operation, all requests pass through
     * - OPEN: Circuit is open, requests fail immediately (fast fail)
     * - HALF_OPEN: Testing if service recovered, limited requests allowed
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        var config = CircuitBreakerConfig.custom()
            // Open circuit if 50% of requests fail
            .failureRateThreshold(50)
            
            // Minimum number of calls before calculating failure rate
            .minimumNumberOfCalls(5)
            
            // Wait 60 seconds before attempting to recover
            .waitDurationInOpenState(Duration.ofSeconds(60))
            
            // Allow 3 test calls in HALF_OPEN state
            .permittedNumberOfCallsInHalfOpenState(3)
            
            // Use a sliding window of last 10 calls
            .slidingWindowSize(10)
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            
            // Open circuit if calls take longer than 5 seconds
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            
            // Record these as failures
            .recordExceptions(
                java.net.ConnectException.class,
                java.net.SocketTimeoutException.class,
                java.util.concurrent.TimeoutException.class,
                org.springframework.web.client.HttpServerErrorException.class
            )
            
            // Don't record these as failures (client errors)
            .ignoreExceptions(
                IllegalArgumentException.class,
                IllegalStateException.class
            )
            
            .build();
        
        var registry = CircuitBreakerRegistry.of(config);
        
        // Create named circuit breakers
        registry.circuitBreaker("llm");
        registry.circuitBreaker("vectorstore");
        registry.circuitBreaker("external-api");
        
        return registry;
    }
    
    /**
     * Retry configuration for transient failures.
     * 
     * Automatically retries failed requests with exponential backoff.
     * Essential for handling temporary network issues or rate limits.
     */
    @Bean
    public RetryRegistry retryRegistry() {
        var config = RetryConfig.custom()
            // Maximum 3 attempts (1 initial + 2 retries)
            .maxAttempts(3)
            
            // Exponential backoff: 1s, 2s, 4s
            .waitDuration(Duration.ofSeconds(1))
            .exponentialBackoffMultiplier(2)
            
            // Retry on these exceptions
            .retryExceptions(
                java.net.SocketTimeoutException.class,
                java.util.concurrent.TimeoutException.class,
                org.springframework.web.client.HttpServerErrorException.ServiceUnavailable.class
            )
            
            // Don't retry on these (permanent failures)
            .ignoreExceptions(
                IllegalArgumentException.class,
                org.springframework.web.client.HttpClientErrorException.BadRequest.class,
                org.springframework.web.client.HttpClientErrorException.Unauthorized.class,
                org.springframework.web.client.HttpClientErrorException.Forbidden.class
            )
            
            // Custom retry condition
            .retryOnResult(response -> {
                // Retry on 429 (Too Many Requests) or 503 (Service Unavailable)
                if (response instanceof org.springframework.http.ResponseEntity<?> re) {
                    return re.getStatusCode().is5xxServerError() ||
                           re.getStatusCode().value() == 429;
                }
                return false;
            })
            
            .build();
        
        var registry = RetryRegistry.of(config);
        
        // Create named retry configurations
        registry.retry("llm");
        registry.retry("vectorstore");
        registry.retry("fast-retry", RetryConfig.custom()
            .maxAttempts(2)
            .waitDuration(Duration.ofMillis(500))
            .build());
        
        return registry;
    }
    
    /**
     * Rate limiter to prevent overwhelming backend services.
     * 
     * Implements token bucket algorithm to smooth out request bursts
     * and protect against runaway agents.
     */
    @Bean
    public RateLimiterRegistry rateLimiterRegistry() {
        // Default: 10 requests per minute per user
        var userLimitConfig = RateLimiterConfig.custom()
            .limitForPeriod(10)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ZERO)  // Fail immediately if limit exceeded
            .build();
        
        // Global: 100 requests per second
        var globalLimitConfig = RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofSeconds(1))
            .timeoutDuration(Duration.ofMillis(100))  // Wait up to 100ms for token
            .build();
        
        // Premium tier: 50 requests per minute
        var premiumLimitConfig = RateLimiterConfig.custom()
            .limitForPeriod(50)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(1))
            .build();
        
        var registry = RateLimiterRegistry.of(userLimitConfig);
        
        registry.rateLimiter("user-tier");
        registry.rateLimiter("global-tier", globalLimitConfig);
        registry.rateLimiter("premium-tier", premiumLimitConfig);
        
        return registry;
    }
    
    /**
     * Time limiter to prevent requests from hanging indefinitely.
     * 
     * Enforces maximum execution time for async operations.
     */
    @Bean
    public TimeLimiterConfig timeLimiterConfig() {
        return TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(30))  // 30 second timeout
            .cancelRunningFuture(true)  // Cancel the operation on timeout
            .build();
    }
}

/**
 * USAGE EXAMPLES
 * 
 * 1. Circuit Breaker on service method:
 * 
 * @Service
 * class RecipeService {
 *     
 *     @CircuitBreaker(name = "llm", fallbackMethod = "fallbackRecipe")
 *     public String generateRecipe(String ingredients) {
 *         return chatClient.prompt()
 *             .user("Create recipe with: " + ingredients)
 *             .call()
 *             .content();
 *     }
 *     
 *     private String fallbackRecipe(String ingredients, Exception e) {
 *         log.warn("LLM unavailable, using fallback", e);
 *         return "Service temporarily unavailable. Please try again in a moment.";
 *     }
 * }
 * 
 * 
 * 2. Retry + Circuit Breaker:
 * 
 * @Retry(name = "llm")
 * @CircuitBreaker(name = "llm", fallbackMethod = "fallback")
 * public String resilientCall() {
 *     // First: Retry up to 3 times with exponential backoff
 *     // Then: If still failing, circuit breaker opens
 *     // Finally: If circuit open, fallback is called
 *     return externalService.call();
 * }
 * 
 * 
 * 3. Rate Limiter:
 * 
 * @RateLimiter(name = "user-tier")
 * public String rateLimitedEndpoint() {
 *     // Throws RequestNotPermitted if rate limit exceeded
 *     return expensiveOperation();
 * }
 * 
 * 
 * 4. Programmatic usage:
 * 
 * var circuitBreaker = circuitBreakerRegistry.circuitBreaker("llm");
 * var rateLimiter = rateLimiterRegistry.rateLimiter("user-tier");
 * 
 * String result = Decorators.ofSupplier(() -> callLLM())
 *     .withCircuitBreaker(circuitBreaker)
 *     .withRateLimiter(rateLimiter)
 *     .withRetry(Retry.of("llm", retryConfig))
 *     .withFallback(List.of(Exception.class), e -> "Fallback response")
 *     .get();
 * 
 * 
 * 5. Monitoring circuit breaker state:
 * 
 * circuitBreaker.getEventPublisher()
 *     .onStateTransition(event -> 
 *         log.warn("Circuit breaker state changed: {} -> {}",
 *             event.getStateTransition().getFromState(),
 *             event.getStateTransition().getToState())
 *     );
 * 
 * 
 * PRODUCTION TIPS:
 * 
 * 1. Tune thresholds based on SLAs:
 *    - Lower failure threshold for critical services
 *    - Longer wait duration for services with known recovery time
 * 
 * 2. Different configs for different services:
 *    - LLM: Higher timeout (30s), more retries
 *    - Vector DB: Lower timeout (5s), fewer retries
 *    - External APIs: Custom per-API configuration
 * 
 * 3. Monitor and alert:
 *    - Circuit breaker state changes
 *    - Retry exhaustion
 *    - Rate limit violations
 *    - Slow call rates
 * 
 * 4. Use bulkhead pattern:
 *    - Separate thread pools for different services
 *    - Prevent one slow service from blocking others
 * 
 * 5. Test failure scenarios:
 *    - Simulate network failures
 *    - Test fallback responses
 *    - Verify circuit breaker behavior
 *    - Load test rate limiters
 */

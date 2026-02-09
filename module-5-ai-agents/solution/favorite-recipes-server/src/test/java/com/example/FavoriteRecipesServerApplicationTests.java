package com.example;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@SpringBootTest(properties = "spring.ai.enabled=false")
class FavoriteRecipesServerApplicationTests {

    @Test
    void contextLoads() {}

    @TestConfiguration
    static class TestConfig {
        @Bean EmbeddingModel embeddingModel() { return mock(EmbeddingModel.class); }
    }
}

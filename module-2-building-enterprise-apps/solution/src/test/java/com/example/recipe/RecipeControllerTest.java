package com.example.recipe;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = RecipeController.class, properties = "spring.ai.enabled=false")
class RecipeControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RecipeService recipeService;

    @Test
    void fetchRecipeFor_returnsJson() throws Exception {
        var recipe = new Recipe("name", "desc", List.of("a"), List.of("step"), "img");
        when(recipeService.fetchRecipeFor(anyList())).thenReturn(recipe);

        mockMvc.perform(get("/api/v1/recipes").param("ingredients", "a", "b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("name"))
                .andExpect(jsonPath("$.ingredients[0]").value("a"));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean ChatClient.Builder chatClientBuilder() {
            var builder = mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
            var chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
            when(builder.defaultSystem(any(Resource.class))).thenReturn(builder);
            when(builder.build()).thenReturn(chatClient);
            return builder;
        }
    }
}

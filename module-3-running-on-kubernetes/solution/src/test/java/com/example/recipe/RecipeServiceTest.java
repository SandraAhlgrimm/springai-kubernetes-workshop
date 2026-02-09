package com.example.recipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    ChatClient chatClient;

    @Mock
    VectorStore vectorStore;

    @Test
    void fetchRecipeFor_default_returnsRecipe() {
        var service = new RecipeService(chatClient, Optional.<ImageModel>empty(), vectorStore);
        ReflectionTestUtils.setField(service, "recipeForIngredientsPromptResource", new ByteArrayResource("".getBytes()));
        var expected = new Recipe("name", "desc", List.of(), List.of(), "img");
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(any(Consumer.class)).call().entity(eq(Recipe.class))).thenReturn(expected);

        var result = service.fetchRecipeFor(List.of("x"), false, false);
        assertEquals(expected, result);
    }

    @Test
    void tool_fetchIngredientsAvailableAtHome_returnsConfigured() {
        var service = new RecipeService(chatClient, Optional.<ImageModel>empty(), vectorStore);
        var available = List.of("eggs", "milk");
        ReflectionTestUtils.setField(service, "availableIngredientsInFridge", available);

        assertEquals(available, service.fetchIngredientsAvailableAtHome());
    }
}

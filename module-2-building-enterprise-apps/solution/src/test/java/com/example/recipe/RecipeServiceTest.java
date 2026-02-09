package com.example.recipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
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

    @Test
    void fetchRecipeFor_returnsRecipe() {
        var expected = new Recipe("name", "desc", List.of("a"), List.of("step"), "img");
        var promptSpec = mock(ChatClient.ChatClientRequestSpec.class, Answers.RETURNS_DEEP_STUBS);
        when(chatClient.prompt()).thenReturn(promptSpec);
        when(promptSpec.user(any(Consumer.class)).call().entity(eq(Recipe.class))).thenReturn(expected);

        var service = new RecipeService(chatClient);
        var result = service.fetchRecipeFor(List.of("a", "b"));

        assertEquals(expected, result);
    }
}

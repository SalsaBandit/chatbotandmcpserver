package com.example.mcpclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class mcpControllerTest {
    private MockMvc mockMvc;
    private DynamicMcpService dynamicMcpService;

    @BeforeEach
    void setUp() {
        // The controller is isolated: the chat client and dynamic transport are mocked at their boundary.
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt(any(Prompt.class)).tools(anyList()).call().content()).thenReturn("Mock reply");
        dynamicMcpService = mock(DynamicMcpService.class);
        when(dynamicMcpService.tools(any())).thenReturn(List.<ToolCallback>of(mock(ToolCallback.class)));
        mockMvc = MockMvcBuilders.standaloneSetup(new mcpController(chatClient, dynamicMcpService))
                .build();
    }

    @Test
    void expenseUsesMcpToolsWithoutApplicationLogin() throws Exception {
        // The request succeeds when a session exposes MCP tools; no separate application identity is required.
        mockMvc.perform(post("/expense")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Mock reply"));
    }

    @Test
    void oauthCallbackReturnsToChatbot() throws Exception {
        // A completed OAuth callback restores connection state, then sends the user back to the browser UI.
        mockMvc.perform(get("/login/oauth2/code/mcp-connection"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"));

        verify(dynamicMcpService).restoreConnection(any(), eq("mcp-connection"));
    }
}

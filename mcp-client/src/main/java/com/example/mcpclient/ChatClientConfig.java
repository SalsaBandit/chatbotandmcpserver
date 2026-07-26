package com.example.mcpclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    ChatClient chatClient(ChatClient.Builder chatBuilder) {
        // DynamicMcpService supplies tools per request, so this base client stays transport-agnostic.
        return chatBuilder.build();
    }
}

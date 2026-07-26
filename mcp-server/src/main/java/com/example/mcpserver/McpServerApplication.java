package com.example.mcpserver;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class McpServerApplication {
	public static void main(String[] args) {
		// Starts the OAuth authorization server, protected MCP endpoint, and database-backed tool layer.
		SpringApplication.run(McpServerApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider tools(ExpenseTools expenseTools) {
		// Spring AI publishes each @Tool method as an MCP tool definition on the /mcp transport.
		return MethodToolCallbackProvider.builder()
				.toolObjects(expenseTools)
				.build();
	}
}

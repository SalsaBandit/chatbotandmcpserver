package com.example.mcpserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
@AutoConfigureMockMvc
class McpServerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RouterFunction<ServerResponse> webMvcStatelessServerRouterFunction;

	@Test
	void contextLoads() {
	}

	@Test
	void exposesTheMcpRouter() {
		// The Spring AI starter creates this router from the ToolCallbackProvider registered at startup.
		org.junit.jupiter.api.Assertions.assertNotNull(webMvcStatelessServerRouterFunction);
	}

	@Test
	void mapsMcpRequestsToTheStreamableTransport() {
		// MCP clients post JSON-RPC messages to /mcp; this confirms the transport accepts that route.
		var request = new MockHttpServletRequest("POST", "/mcp");
		request.setContentType(MediaType.APPLICATION_JSON_VALUE);

		org.junit.jupiter.api.Assertions.assertTrue(
				webMvcStatelessServerRouterFunction.route(ServerRequest.create(request, List.of())).isPresent());
	}

	@Test
	void publishesTheAuthorizationServerInProtectedResourceMetadata() throws Exception {
		// Clients use this public metadata to find OAuth and the mcp.tools scope before authorization.
		mockMvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.authorization_servers[0]").value("http://localhost:8060"))
				.andExpect(jsonPath("$.scopes_supported[0]").value("mcp.tools"));
	}
}

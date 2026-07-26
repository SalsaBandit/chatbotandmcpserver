package com.example.mcpclient;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
public class mcpController {
    private final ChatClient chatClient;
    private final DynamicMcpService dynamicMcpService;

    public mcpController(ChatClient chatClient, DynamicMcpService dynamicMcpService) {
        this.chatClient = chatClient;
        this.dynamicMcpService = dynamicMcpService;
    }

    @GetMapping("/login/oauth2/code/{registrationId}")
    public ResponseEntity<Void> oauthCallback(
            @org.springframework.web.bind.annotation.PathVariable String registrationId,
            jakarta.servlet.http.HttpServletRequest request) {
        // Spring Security has exchanged the authorization code for tokens before this callback completes.
        dynamicMcpService.restoreConnection(request.getSession(), registrationId);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/")
                .build();
    }

    @PostMapping("/expense")
    public ResponseEntity<String> getExpense(
            @RequestBody String prompt,
            jakarta.servlet.http.HttpServletRequest request) {
        // The browser's text becomes the AI prompt; the connected MCP servers supply its available tools.
        PromptTemplate pt = new PromptTemplate(prompt);
        List<org.springframework.ai.tool.ToolCallback> tools = dynamicMcpService.tools(request.getSession());
        if (tools.isEmpty()) {
            return ResponseEntity.badRequest().body("Add and authorize an MCP server before using its tools.");
        }
        // The model chooses tools as needed, calls them through DynamicMcpService, then returns its final answer.
        return ResponseEntity.ok(this.chatClient.prompt(pt.create()).tools(tools).call().content());
    }

    @org.springframework.web.bind.annotation.PostMapping("/mcp/servers")
    public ResponseEntity<?> addMcpServer(
            @RequestBody AddMcpServerRequest request,
            jakarta.servlet.http.HttpServletRequest servletRequest) {
        try {
            // Derive the public base URL from this request so the OAuth provider returns to this client instance.
            String baseUrl = ServletUriComponentsBuilder.fromRequestUri(servletRequest)
                    .replacePath(null)
                    .replaceQuery(null)
                    .build()
                    .toUriString();
            var connection = dynamicMcpService.add(
                    servletRequest.getSession(),
                    request.name(),
                    request.serverUrl(),
                    baseUrl);
            return ResponseEntity.status(HttpStatus.CREATED).body(connection);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
        }
    }

    @GetMapping("/mcp/servers")
    public List<DynamicMcpService.ConnectionResponse> listMcpServers(jakarta.servlet.http.HttpServletRequest request) {
        // script.js uses this response to render connection status and authorization links.
        return dynamicMcpService.list(request);
    }

    @GetMapping("/mcp/servers/{id}/authorize")
    public ResponseEntity<Void> authorizeMcpServer(
            @org.springframework.web.bind.annotation.PathVariable String id,
            jakarta.servlet.http.HttpServletRequest request) {
        return dynamicMcpService.list(request).stream()
                .filter(connection -> connection.id().equals(id))
                .findFirst()
                // The local URL delegates the actual provider redirect to Spring Security's OAuth endpoint.
                .map(connection -> ResponseEntity.status(HttpStatus.FOUND)
                        .header(HttpHeaders.LOCATION, "/oauth2/authorization/mcp-" + id)
                        .<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    public record AddMcpServerRequest(String name, String serverUrl) {
    }

    public record ErrorResponse(String message) {
    }
}

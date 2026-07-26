package com.example.mcpclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication

public class McpClientApplication {
    public static void main(String[] args) {
        // Boots the browser UI, REST API, OAuth client, and AI chat client in one process.
        SpringApplication.run(McpClientApplication.class, args);
    }

}

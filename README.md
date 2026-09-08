# Chatbot and MCP Server

A comprehensive Java-based application demonstrating a chatbot system with Model Context Protocol (MCP) support, featuring both a dedicated MCP server and client implementation using Spring Boot and Spring AI.

## Overview

This repository contains two primary modules:

- **mcp-server**: A Spring Boot application that implements the Model Context Protocol server-side, enabling secure resource sharing and AI model integration with OAuth2 authorization.
- **mcp-client**: A Spring Boot web application that serves as a chatbot client, consuming MCP server resources with OAuth2 client authentication and Ollama AI model integration.

## Project Structure

```
chatbotandmcpserver/
├── mcp-server/              # MCP Server Implementation
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/       # Server-side Java source code
│   │   │   └── resources/  # Configuration files
│   │   └── test/           # Unit tests
│   ├── build.gradle.kts    # Server build configuration
│   └── gradle/             # Gradle wrapper files
│
└── mcp-client/              # Chatbot Client Implementation
    ├── src/
    │   ├── main/
    │   │   ├── java/       # Client-side Java source code
    │   │   └── resources/  # Configuration files
    │   └── test/           # Unit tests
    ├── build.gradle.kts    # Client build configuration
    └── gradle/             # Gradle wrapper files
```

## Technology Stack

### Core Technologies
- **Java 26**: Modern Java with latest features
- **Spring Boot 4.1.0**: Full-stack application framework
- **Spring AI 2.0.0**: AI/LLM integration framework
- **Spring Security**: Authentication and authorization
- **OAuth2**: Secure authorization protocol

### Server (mcp-server)
- **Spring Boot Starter Data JPA**: Database persistence
- **Spring AI MCP Server**: Model Context Protocol server implementation
- **Spring Boot Security**: Server-side authentication
- **OAuth2 Resource Server**: Secure resource protection
- **OAuth2 Authorization Server**: Token issuance and management
- **PostgreSQL**: Production database
- **MCP Server Security**: Custom MCP-specific security library (v0.1.13)

### Client (mcp-client)
- **Spring Boot Web MVC**: Web application framework
- **Spring Boot Security**: Client-side authentication
- **OAuth2 Client**: OAuth2 client authentication flow
- **Spring AI MCP Client**: Model Context Protocol client
- **Ollama AI Starter**: Local LLM model integration (Ollama)
- **MCP Client Security**: Custom MCP-specific security library (v0.1.13)

## Language Composition

- **Java**: 80.5%
- **JavaScript**: 8.4%
- **CSS**: 7.6%
- **HTML**: 3.5%

## Getting Started

### Prerequisites

- Java 26 JDK or higher
- Gradle (included via wrapper)
- PostgreSQL (for mcp-server runtime)
- Ollama (for mcp-client LLM integration)

### Building

#### MCP Server
```bash
cd mcp-server
./gradlew build
```

#### MCP Client
```bash
cd mcp-client
./gradlew build
```

### Running

#### MCP Server
```bash
cd mcp-server
./gradlew bootRun
```

#### MCP Client
```bash
cd mcp-client
./gradlew bootRun
```

## Features

### MCP Server
- **Secure Resource Sharing**: OAuth2-protected resources
- **Authorization Management**: Full authorization server implementation
- **MCP Protocol Support**: Spring AI MCP server starter
- **Data Persistence**: JPA/Hibernate with PostgreSQL backend
- **RESTful APIs**: Built on Spring Web MVC

### MCP Client
- **Web-based Interface**: Interactive chatbot UI
- **OAuth2 Authentication**: Secure client authentication
- **Local LLM Integration**: Ollama AI model support
- **MCP Client Protocol**: Full MCP client implementation
- **Dynamic OAuth Support**: MCP-specific OAuth configuration

## Configuration

Configuration files are typically located in `src/main/resources/` for each module (application.yml, application.properties, etc.).

### Key Configuration Areas
- Database connection (server)
- OAuth2 credentials and endpoints
- Ollama model configuration (client)
- MCP server/client settings

## Testing

Both modules include comprehensive test suites.

### Run All Tests
```bash
# Server tests
cd mcp-server && ./gradlew test

# Client tests
cd mcp-client && ./gradlew test
```

## Dependencies

### Shared Dependencies
- Spring Boot Test Framework
- JUnit Platform
- Spring Data JPA Test utilities

### Build Tools
- Gradle 8.x with Kotlin DSL
- Spring Boot Gradle Plugin
- Spring Dependency Management Plugin

## Architecture

The application follows a client-server architecture:

1. **MCP Server**: Exposes resources and AI capabilities via the Model Context Protocol
2. **Security Layer**: OAuth2 handles authentication and authorization
3. **MCP Client**: Web application that consumes server resources and provides user interface
4. **AI Integration**: Ollama backend for local LLM processing

## Development

Both modules use:
- **Java Module Structure**: Package-based organization (com.example.*)
- **Gradle Build System**: Kotlin DSL for configuration
- **Spring Boot Conventions**: Standard Spring project layout
- **Maven Repository**: Central Maven repository for dependencies

## Contributing

When contributing to this project:
1. Follow Spring Boot conventions
2. Maintain Java 26+ compatibility
3. Include unit tests for new features
4. Use Gradle for building and testing

## License

This project is currently unlicensed. Please check with the project maintainers for license information.

## Repository Info

- **Owner**: SalsaBandit
- **Repository URL**: https://github.com/SalsaBandit/chatbotandmcpserver
- **Default Branch**: main
- **Created**: 44 days ago
- **Last Updated**: 44 days ago

## Support

For issues, questions, or contributions, please refer to the GitHub repository.

---

**Note**: This README is auto-generated based on the repository structure and build configuration. For detailed API documentation, configuration options, and usage examples, please refer to the source code and Spring AI documentation.

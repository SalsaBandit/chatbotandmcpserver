package com.example.mcpserver;

import org.jspecify.annotations.Nullable;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DynamicRegisteredClientRepository implements RegisteredClientRepository {
    // The authorization server resolves a client by either Spring's internal ID or its public OAuth client_id.
    private final Map<String, RegisteredClient> clientsById = new ConcurrentHashMap<>();
    private final Map<String, RegisteredClient> clientsByClientId = new ConcurrentHashMap<>();

    @Override
    public void save(RegisteredClient registeredClient) {
        // DCR writes both indexes for this in-memory, single-process demonstration.
        Assert.notNull(registeredClient, "registeredClient cannot be null");
        clientsById.put(registeredClient.getId(), registeredClient);
        clientsByClientId.put(registeredClient.getClientId(), registeredClient);
    }

    @Override
    public @Nullable RegisteredClient findById(String id) {
        // Used internally by Spring Authorization Server.
        return clientsById.get(id);
    }

    @Override
    public @Nullable RegisteredClient findByClientId(String clientId) {
        // Used when the token endpoint authenticates a dynamically registered client.
        return clientsByClientId.get(clientId);
    }
}

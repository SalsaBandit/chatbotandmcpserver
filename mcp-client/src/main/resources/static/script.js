const chatMessages = document.getElementById('chatMessages');
const userInput = document.getElementById('userInput');
const sendBtn = document.getElementById('sendBtn');
const authStatus = document.getElementById('authStatus');
const mcpForm = document.getElementById('mcpForm');
const mcpName = document.getElementById('mcpName');
const mcpUrl = document.getElementById('mcpUrl');
const mcpError = document.getElementById('mcpError');
const mcpBtn = document.getElementById('mcpBtn');
const mcpList = document.getElementById('mcpList');

function addMessage(text, isUser = true) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'bot'}`;
    messageDiv.textContent = text;
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function addThinkingMessage() {
    const messageDiv = document.createElement('div');
    messageDiv.className = 'message loading';
    messageDiv.id = 'thinkingMessage';
    messageDiv.textContent = 'Thinking...';
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function removeThinkingMessage() {
    document.getElementById('thinkingMessage')?.remove();
}

function setMcpError(message = '') {
    mcpError.textContent = message;
    mcpError.hidden = !message;
}

function renderConnections(connections) {
    // Server responses contain only metadata and a local authorization URL, never OAuth tokens.
    mcpList.replaceChildren();
    mcpList.hidden = connections.length === 0;
    connections.forEach((connection) => {
        const item = document.createElement('div');
        item.className = 'mcp-connection';
        const name = document.createElement('span');
        name.textContent = connection.name;
        const status = document.createElement(connection.authorized ? 'span' : 'a');
        status.className = connection.authorized ? 'mcp-status' : '';
        if (connection.authorized) {
            status.textContent = 'Authorized';
        } else {
            // Following this local link starts Spring Security's redirect to the MCP server's OAuth login.
            status.href = connection.authorizeUrl;
            status.textContent = 'Authorize';
        }
        item.append(name, status);
        mcpList.appendChild(item);
    });
}

async function loadConnections() {
    // Restore the visible connection state whenever the page is loaded after an OAuth redirect.
    const response = await fetch('/mcp/servers');
    if (!response.ok) {
        throw new Error('Unable to load MCP servers');
    }
    renderConnections(await response.json());
}

async function initializeSession() {
    await loadConnections();
}

async function addMcpServer(event) {
    event.preventDefault();
    setMcpError('');
    mcpBtn.disabled = true;
    try {
        // Persist the server metadata first; the response tells the browser where to start OAuth.
        const response = await fetch('/mcp/servers', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name: mcpName.value.trim(), serverUrl: mcpUrl.value.trim() }),
        });
        const result = await response.json();
        if (!response.ok) {
            throw new Error(result.message || 'Unable to add MCP server');
        }
        // Navigation leaves the UI for consent/login and eventually returns to the callback controller.
        window.location.assign(result.authorizeUrl);
    } catch (error) {
        setMcpError(error.message);
    } finally {
        mcpBtn.disabled = false;
    }
}

async function sendMessage() {
    const message = userInput.value.trim();
    if (!message) return;
    addMessage(message, true);
    userInput.value = '';
    sendBtn.disabled = true;
    addThinkingMessage();
    try {
        // The prompt is sent as plain text; the server gives the AI the authenticated MCP tools for this session.
        const response = await fetch('/expense', {
            method: 'POST',
            headers: { 'Content-Type': 'text/plain' },
            body: message,
        });
        const text = await response.text();
        if (!response.ok) throw new Error(text);
        removeThinkingMessage();
        addMessage(text, false);
    } catch (error) {
        removeThinkingMessage();
        addMessage(`Error: ${error.message}`, false);
    } finally {
        sendBtn.disabled = false;
        userInput.focus();
    }
}

sendBtn.addEventListener('click', sendMessage);
mcpForm.addEventListener('submit', addMcpServer);
userInput.addEventListener('keypress', (event) => {
    if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault();
        sendMessage();
    }
});

// Initial loading detects any connections that were retained through an OAuth round trip.
initializeSession().catch((error) => {
    authStatus.textContent = error.message;
});

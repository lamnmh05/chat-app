package com.doan.frontend.service;

import com.doan.frontend.model.ws.ChannelSocketEventResponse;
import com.doan.frontend.model.ws.ChatMessageSocketRequest;
import com.doan.frontend.model.ws.DirectMessageSocketRequest;
import com.doan.frontend.model.ws.DirectSocketEventResponse;
import com.doan.frontend.model.ws.PresenceUpdateSocketRequest;
import com.doan.frontend.model.ws.TypingSocketRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StompWebSocketService implements WebSocket.Listener {
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Supplier<String> tokenSupplier;
    private final String baseUrl;
    private final AtomicInteger subscriptionCounter = new AtomicInteger(1);
    private final Map<String, Subscription<?>> subscriptions = new ConcurrentHashMap<>();
    private final StringBuilder frameBuffer = new StringBuilder();
    private final Object sendLock = new Object();
    private volatile WebSocket webSocket;
    private volatile CompletableFuture<Void> connectFuture;
    private volatile Consumer<Throwable> errorHandler;
    private volatile boolean stompConnected;

    public StompWebSocketService(ObjectMapper objectMapper, Supplier<String> tokenSupplier, String baseUrl) {
        this.objectMapper = objectMapper;
        this.tokenSupplier = tokenSupplier;
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    public void setErrorHandler(Consumer<Throwable> errorHandler) {
        this.errorHandler = errorHandler;
    }

    public CompletableFuture<Void> connect() {
        if (isConnected()) {
            return CompletableFuture.completedFuture(null);
        }
        if (connectFuture != null && !connectFuture.isDone()) {
            return connectFuture;
        }
        stompConnected = false;
        connectFuture = new CompletableFuture<>();
        httpClient.newWebSocketBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .buildAsync(URI.create(toWebSocketUrl()), this)
            .whenComplete((socket, throwable) -> {
                if (throwable != null) {
                    this.webSocket = null;
                    connectFuture.completeExceptionally(throwable);
                    notifyError(throwable);
                } else {
                    this.webSocket = socket;
                }
            });
        return connectFuture;
    }

    public boolean isConnected() {
        return stompConnected
            && webSocket != null
            && !webSocket.isInputClosed()
            && !webSocket.isOutputClosed();
    }

    public SubscriptionHandle subscribeChannel(String channelId, Consumer<ChannelSocketEventResponse> consumer) {
        return subscribe("/topic/channel/" + channelId, ChannelSocketEventResponse.class, consumer);
    }

    public SubscriptionHandle subscribePresence(String guildId, Consumer<ChannelSocketEventResponse> consumer) {
        return subscribe("/topic/presence/" + guildId, ChannelSocketEventResponse.class, consumer);
    }

    public SubscriptionHandle subscribeDirectMessages(Consumer<DirectSocketEventResponse> consumer) {
        return subscribe("/user/queue/direct-messages", DirectSocketEventResponse.class, consumer);
    }

    public void sendMessage(ChatMessageSocketRequest request) {
        sendJsonFrame("/app/chat.sendMessage", request);
    }

    public void sendTyping(TypingSocketRequest request) {
        sendJsonFrame("/app/chat.typing", request);
    }

    public void sendPresence(PresenceUpdateSocketRequest request) {
        sendJsonFrame("/app/presence.update", request);
    }

    public void sendDirectMessage(DirectMessageSocketRequest request) {
        sendJsonFrame("/app/direct.sendMessage", request);
    }

    public void disconnect() {
        if (webSocket == null) {
            return;
        }
        stompConnected = false;
        sendRaw("DISCONNECT\n\n\0");
        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        webSocket = null;
        connectFuture = null;
        subscriptions.clear();
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
        this.webSocket = webSocket;
        webSocket.request(1);
        sendConnectFrame();
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        frameBuffer.append(data);
        if (last) {
            String payload = frameBuffer.toString();
            frameBuffer.setLength(0);
            for (String frame : payload.split("\0")) {
                if (!frame.isBlank()) {
                    handleFrame(frame);
                }
            }
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        stompConnected = false;
        this.webSocket = null;
        notifyError(error);
        if (connectFuture != null && !connectFuture.isDone()) {
            connectFuture.completeExceptionally(error);
        }
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        stompConnected = false;
        this.webSocket = null;
        if (connectFuture != null && !connectFuture.isDone()) {
            connectFuture.completeExceptionally(
                new IllegalStateException("WebSocket closed before STOMP CONNECTED: " + statusCode + " " + reason)
            );
        }
        return CompletableFuture.completedFuture(null);
    }

    private void sendConnectFrame() {
        String token = tokenSupplier.get();
        StringBuilder frame = new StringBuilder();
        frame.append("CONNECT\n");
        frame.append("accept-version:1.2\n");
        frame.append("heart-beat:10000,10000\n");
        if (token != null && !token.isBlank()) {
            frame.append("authorization:Bearer ").append(token).append('\n');
        }
        frame.append('\n');
        frame.append('\0');
        sendRaw(frame.toString());
    }

    private SubscriptionHandle subscribe(String destination, Class<?> payloadType, Consumer<?> consumer) {
        ensureConnected();
        String subscriptionId = "sub-" + subscriptionCounter.getAndIncrement();
        subscriptions.put(subscriptionId, new Subscription<>(destination, payloadType, consumer));
        String frame = "SUBSCRIBE\nid:" + subscriptionId + "\ndestination:" + destination + "\n\n\0";
        sendRaw(frame);
        return () -> unsubscribe(subscriptionId);
    }

    private void unsubscribe(String subscriptionId) {
        if (subscriptions.remove(subscriptionId) != null) {
            sendRaw("UNSUBSCRIBE\nid:" + subscriptionId + "\n\n\0");
        }
    }

    private void sendJsonFrame(String destination, Object payload) {
        ensureConnected();
        try {
            String body = objectMapper.writeValueAsString(payload);
            String frame = "SEND\ndestination:" + destination + "\ncontent-type:application/json\n\n" + body + '\0';
            sendRaw(frame);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize WebSocket payload", exception);
        }
    }

    private void sendRaw(String frame) {
        synchronized (sendLock) {
            if (webSocket == null) {
                throw new IllegalStateException("WebSocket is not connected");
            }
            webSocket.sendText(frame, true);
        }
    }

    private void handleFrame(String frame) {
        ParsedFrame parsedFrame = parseFrame(frame);
        switch (parsedFrame.command()) {
            case "CONNECTED" -> {
                stompConnected = true;
                if (connectFuture != null && !connectFuture.isDone()) {
                    connectFuture.complete(null);
                }
            }
            case "MESSAGE" -> dispatchMessage(parsedFrame);
            case "ERROR" -> {
                stompConnected = false;
                IllegalStateException error = new IllegalStateException(parsedFrame.body());
                notifyError(error);
                if (connectFuture != null && !connectFuture.isDone()) {
                    connectFuture.completeExceptionally(error);
                }
            }
            default -> {
            }
        }
    }

    private void dispatchMessage(ParsedFrame parsedFrame) {
        String subscriptionId = parsedFrame.headers().get("subscription");
        Subscription<?> subscription = subscriptions.get(subscriptionId);
        if (subscription == null) {
            return;
        }
        try {
            Object payload = objectMapper.readValue(parsedFrame.body(), subscription.payloadType());
            ((Consumer<Object>) subscription.consumer()).accept(payload);
        } catch (Exception exception) {
            notifyError(exception);
        }
    }

    private ParsedFrame parseFrame(String rawFrame) {
        String normalized = rawFrame.replace("\r", "");
        int separatorIndex = normalized.indexOf("\n\n");
        String head = separatorIndex >= 0 ? normalized.substring(0, separatorIndex) : normalized;
        String body = separatorIndex >= 0 ? normalized.substring(separatorIndex + 2) : "";
        String[] lines = head.split("\n");
        String command = lines[0];
        Map<String, String> headers = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            int colonIndex = line.indexOf(':');
            if (colonIndex > 0) {
                headers.put(line.substring(0, colonIndex), line.substring(colonIndex + 1));
            }
        }
        return new ParsedFrame(command, headers, body);
    }

    private String toWebSocketUrl() {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalizedBase.startsWith("https://")) {
            return "wss://" + normalizedBase.substring("https://".length()) + "/ws";
        }
        if (normalizedBase.startsWith("http://")) {
            return "ws://" + normalizedBase.substring("http://".length()) + "/ws";
        }
        return normalizedBase + "/ws";
    }

    private void ensureConnected() {
        if (!isConnected()) {
            throw new IllegalStateException("WebSocket is not connected");
        }
    }

    private void notifyError(Throwable throwable) {
        if (errorHandler != null) {
            errorHandler.accept(throwable);
        }
    }

    public interface SubscriptionHandle {
        void unsubscribe();
    }

    private record Subscription<T>(String destination, Class<?> payloadType, Consumer<T> consumer) {
    }

    private record ParsedFrame(String command, Map<String, String> headers, String body) {
    }
}

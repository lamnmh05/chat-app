package com.doan.frontend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StompWebSocketServiceTest {
    @Test
    void onOpenSendsConnectFrameBeforeConnectedEventArrives() {
        StompWebSocketService service = new StompWebSocketService(
            new ObjectMapper(),
            () -> "test-token",
            "http://localhost:8080"
        );
        FakeWebSocket socket = new FakeWebSocket();

        Assertions.assertDoesNotThrow(() -> service.onOpen(socket));
        Assertions.assertFalse(service.isConnected());
        Assertions.assertTrue(socket.requestCount > 0);
        Assertions.assertEquals(1, socket.sentTexts.size());
        Assertions.assertTrue(socket.sentTexts.getFirst().contains("CONNECT"));
        Assertions.assertTrue(socket.sentTexts.getFirst().contains("authorization:Bearer test-token"));

        service.onText(socket, "CONNECTED\nversion:1.2\n\n\0", true);

        Assertions.assertTrue(service.isConnected());
    }

    private static final class FakeWebSocket implements WebSocket {
        private final List<String> sentTexts = new ArrayList<>();
        private int requestCount;
        private boolean inputClosed;
        private boolean outputClosed;

        @Override
        public CompletableFuture<WebSocket> sendText(CharSequence data, boolean last) {
            sentTexts.add(data.toString());
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendBinary(ByteBuffer data, boolean last) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPing(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendPong(ByteBuffer message) {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public CompletableFuture<WebSocket> sendClose(int statusCode, String reason) {
            inputClosed = true;
            outputClosed = true;
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public void request(long n) {
            requestCount += (int) n;
        }

        @Override
        public String getSubprotocol() {
            return null;
        }

        @Override
        public boolean isOutputClosed() {
            return outputClosed;
        }

        @Override
        public boolean isInputClosed() {
            return inputClosed;
        }

        @Override
        public void abort() {
            inputClosed = true;
            outputClosed = true;
        }
    }
}

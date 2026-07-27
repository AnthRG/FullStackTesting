package pucmm.freddy.fullstacktesting.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pucmm.freddy.fullstacktesting.dto.SocketMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mantiene el registro de sesiones abiertas y difunde las alertas a todas.
 *
 * <p>El broadcast nunca propaga errores: una sesión caída se descarta y las demás
 * siguen recibiendo, para no tumbar el hilo que procesa el evento de dominio.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int SEND_BUFFER_SIZE_BYTES = 512 * 1024;

    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // sendMessage no es thread-safe y el broadcast puede coincidir con un PING:
        // el decorador serializa los envios por sesion y corta la que se atasque.
        sessions.put(session.getId(), new ConcurrentWebSocketSessionDecorator(
                session, SEND_TIME_LIMIT_MS, SEND_BUFFER_SIZE_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Error de transporte en la sesion {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session.getId());
    }

    public void broadcast(SocketMessage message) {
        if (sessions.isEmpty()) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (JacksonException ex) {
            log.error("No se pudo serializar el mensaje de WebSocket", ex);
            return;
        }

        for (WebSocketSession session : sessions.values()) {
            send(session, json);
        }
    }

    public int openSessions() {
        return sessions.size();
    }

    private void send(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            sessions.remove(session.getId());
            return;
        }
        try {
            session.sendMessage(new TextMessage(json));
        } catch (IOException | IllegalStateException | SessionLimitExceededException ex) {
            log.debug("No se pudo enviar a la sesion {}: {}", session.getId(), ex.getMessage());
            sessions.remove(session.getId());
        }
    }
}

package pucmm.freddy.fullstacktesting.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pucmm.freddy.fullstacktesting.dto.SocketMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;
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

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Error de transporte en la sesion {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
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

        for (WebSocketSession session : sessions) {
            send(session, json);
        }
    }

    public int openSessions() {
        return sessions.size();
    }

    private void send(WebSocketSession session, String json) {
        if (!session.isOpen()) {
            sessions.remove(session);
            return;
        }
        try {
            // sendMessage no es thread-safe: el broadcast y un PING pueden coincidir.
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException | IllegalStateException ex) {
            log.debug("No se pudo enviar a la sesion {}: {}", session.getId(), ex.getMessage());
            sessions.remove(session);
        }
    }
}

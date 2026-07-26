package pucmm.freddy.fullstacktesting.config;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import pucmm.freddy.fullstacktesting.ws.BearerSubprotocolHandshakeInterceptor;
import pucmm.freddy.fullstacktesting.ws.NotificationWebSocketHandler;

import java.util.List;

@Configuration
@EnableWebSocket
@EnableScheduling
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final BearerSubprotocolHandshakeInterceptor handshakeInterceptor;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(handshakeInterceptor)
                .setHandshakeHandler(handshakeHandler())
                .setAllowedOrigins(allowedOrigins.toArray(String[]::new));
    }

    /**
     * El servidor debe devolver el subprotocolo "bearer" que pidió el cliente; si no
     * lo confirma, el navegador cierra la conexión apenas termina el handshake.
     */
    private DefaultHandshakeHandler handshakeHandler() {
        DefaultHandshakeHandler handler = new DefaultHandshakeHandler();
        handler.setSupportedProtocols(BearerSubprotocolHandshakeInterceptor.BEARER_PROTOCOL);
        return handler;
    }
}

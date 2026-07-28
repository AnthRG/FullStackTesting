package pucmm.freddy.fullstacktesting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Sobre de los mensajes servidor -> cliente del WebSocket. El PING viaja sin
 * payload (se omite del JSON) y solo sirve de keepalive.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocketMessage(

        String type,

        NotificationResponse payload

) {
    public static final String TYPE_NOTIFICATION = "NOTIFICATION";
    public static final String TYPE_PING = "PING";

    public static SocketMessage notification(NotificationResponse payload) {
        return new SocketMessage(TYPE_NOTIFICATION, payload);
    }

    public static SocketMessage ping() {
        return new SocketMessage(TYPE_PING, null);
    }
}

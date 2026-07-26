package pucmm.freddy.fullstacktesting.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Autentica el handshake del WebSocket.
 *
 * <p>El navegador no permite mandar el header {@code Authorization} al abrir un
 * WebSocket, y meter el token en la URL lo dejaría en logs e historial. El cliente
 * lo manda entonces como subprotocolo: {@code new WebSocket(url, ['bearer', token])},
 * que viaja en {@code Sec-WebSocket-Protocol}. Aquí se toma el segundo valor, se
 * valida con el mismo {@link JwtDecoder} del resource server y se exige
 * {@code product:view} usando el mismo mapeo de roles que SecurityConfig.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BearerSubprotocolHandshakeInterceptor implements HandshakeInterceptor {

    public static final String BEARER_PROTOCOL = "bearer";
    public static final String USERNAME_ATTRIBUTE = "username";

    private static final String REQUIRED_AUTHORITY = "ROLE_product:view";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler handler, Map<String, Object> attributes) {
        List<String> protocols = requestedProtocols(request);
        if (protocols.size() < 2 || !BEARER_PROTOCOL.equalsIgnoreCase(protocols.get(0))) {
            return reject(response, HttpStatus.UNAUTHORIZED, "handshake sin subprotocolo bearer");
        }

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(protocols.get(1));
        } catch (JwtException ex) {
            return reject(response, HttpStatus.UNAUTHORIZED, "token invalido: " + ex.getMessage());
        }

        if (!hasRequiredRole(jwt)) {
            return reject(response, HttpStatus.FORBIDDEN, "token sin el rol product:view");
        }

        attributes.put(USERNAME_ATTRIBUTE, jwt.getClaimAsString("preferred_username"));
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler handler, Exception exception) {
        // Sin post-procesamiento: el rechazo ya se resolvio en beforeHandshake.
    }

    private boolean hasRequiredRole(Jwt jwt) {
        AbstractAuthenticationToken authentication = jwtAuthenticationConverter.convert(jwt);
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> REQUIRED_AUTHORITY.equals(authority.getAuthority()));
    }

    /** Sec-WebSocket-Protocol puede venir repetido o como una lista separada por comas. */
    private List<String> requestedProtocols(ServerHttpRequest request) {
        List<String> raw = request.getHeaders().get(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL);
        if (raw == null) return List.of();
        List<String> values = new ArrayList<>();
        for (String header : raw) {
            for (String value : header.split(",")) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) values.add(trimmed);
            }
        }
        return values;
    }

    private boolean reject(ServerHttpResponse response, HttpStatus status, String reason) {
        log.debug("WebSocket handshake rechazado ({}): {}", status.value(), reason);
        response.setStatusCode(status);
        return false;
    }
}

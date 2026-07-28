package pucmm.freddy.fullstacktesting.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BearerSubprotocolHandshakeInterceptorTest {

    @Mock
    private JwtDecoder jwtDecoder;

    @Mock
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    @Mock
    private WebSocketHandler handler;

    @InjectMocks
    private BearerSubprotocolHandshakeInterceptor interceptor;

    private Map<String, Object> attributes;

    @BeforeEach
    void setUp() {
        attributes = new HashMap<>();
    }

    private void withProtocolHeader(String... values) {
        HttpHeaders headers = new HttpHeaders();
        for (String value : values) {
            headers.add(WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL, value);
        }
        when(request.getHeaders()).thenReturn(headers);
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("preferred_username", "admin")
                .build();
    }

    private void withAuthorities(Jwt jwt, String... authorities) {
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(new JwtAuthenticationToken(
                jwt, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()));
    }

    private boolean handshake() throws Exception {
        return interceptor.beforeHandshake(request, response, handler, attributes);
    }

    @Test
    void beforeHandshake_conTokenValidoYPermisoProductView_aceptaYGuardaElUsuario() throws Exception {
        withProtocolHeader("bearer, el-token");
        Jwt jwt = jwt();
        when(jwtDecoder.decode("el-token")).thenReturn(jwt);
        withAuthorities(jwt, "product:view", "product:manage");

        assertThat(handshake()).isTrue();
        assertThat(attributes).containsEntry(
                BearerSubprotocolHandshakeInterceptor.USERNAME_ATTRIBUTE, "admin");
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void beforeHandshake_conElProtocoloEnDosHeadersSeparados_aceptaIgual() throws Exception {
        withProtocolHeader("bearer", "el-token");
        Jwt jwt = jwt();
        when(jwtDecoder.decode("el-token")).thenReturn(jwt);
        withAuthorities(jwt, "product:view");

        assertThat(handshake()).isTrue();
    }

    @Test
    void beforeHandshake_sinHeaderDeSubprotocolo_rechazaCon401() throws Exception {
        when(request.getHeaders()).thenReturn(new HttpHeaders());

        assertThat(handshake()).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void beforeHandshake_conSubprotocoloSinToken_rechazaCon401() throws Exception {
        withProtocolHeader("bearer");

        assertThat(handshake()).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void beforeHandshake_conOtroSubprotocolo_rechazaCon401() throws Exception {
        withProtocolHeader("basic, el-token");

        assertThat(handshake()).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    void beforeHandshake_conTokenInvalido_rechazaCon401() throws Exception {
        withProtocolHeader("bearer, roto");
        when(jwtDecoder.decode("roto")).thenThrow(new BadJwtException("firma invalida"));

        assertThat(handshake()).isFalse();
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        assertThat(attributes).isEmpty();
    }

    @Test
    void beforeHandshake_conTokenValidoSinElPermiso_rechazaCon403() throws Exception {
        withProtocolHeader("bearer, el-token");
        Jwt jwt = jwt();
        when(jwtDecoder.decode("el-token")).thenReturn(jwt);
        withAuthorities(jwt, "VIEW_ROLES");

        assertThat(handshake()).isFalse();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        assertThat(attributes).isEmpty();
    }

    @Test
    void beforeHandshake_cuandoElConverterNoDevuelveAutenticacion_rechazaCon403() throws Exception {
        withProtocolHeader("bearer, el-token");
        Jwt jwt = jwt();
        when(jwtDecoder.decode("el-token")).thenReturn(jwt);
        when(jwtAuthenticationConverter.convert(jwt)).thenReturn(null);

        assertThat(handshake()).isFalse();
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
    }

    @Test
    void afterHandshake_noHaceNadaNiFalla() {
        interceptor.afterHandshake(request, response, handler, null);

        verifyNoInteractions(response);
    }
}

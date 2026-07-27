package pucmm.freddy.fullstacktesting.ws;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.dto.StockMovementRequest;
import pucmm.freddy.fullstacktesting.service.ProductService;
import pucmm.freddy.fullstacktesting.service.StockMovementService;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationWebSocketIT extends AbstractIntegrationTest {

    private static final int TIMEOUT_SECONDS = 15;

    @LocalServerPort
    private int port;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockMovementService stockMovementService;

    private final List<WebSocketSession> abiertas = new ArrayList<>();

    @AfterEach
    void cerrarSesiones() throws Exception {
        for (WebSocketSession session : abiertas) {
            if (session.isOpen()) session.close(CloseStatus.NORMAL);
        }
        abiertas.clear();
    }

    /** Acumula lo que llega del servidor para poder esperarlo desde el test. */
    private static class Buzon extends TextWebSocketHandler {
        private final BlockingQueue<String> mensajes = new LinkedBlockingQueue<>();

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            mensajes.add(message.getPayload());
        }

        String esperarNotificacion() throws InterruptedException {
            long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
            while (System.nanoTime() < limite) {
                String mensaje = mensajes.poll(1, TimeUnit.SECONDS);
                // Se ignora cualquier PING de keepalive que se cuele.
                if (mensaje != null && mensaje.contains("\"type\":\"NOTIFICATION\"")) return mensaje;
            }
            return null;
        }
    }

    private WebSocketSession conectar(Buzon buzon, String... subprotocolos) throws Exception {
        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.setSecWebSocketProtocol(List.of(subprotocolos));
        WebSocketSession session = new StandardWebSocketClient()
                .execute(buzon, headers, URI.create("ws://localhost:" + port + "/ws/notifications"))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        abiertas.add(session);
        return session;
    }

    private ProductResponse crearProducto(String token, int quantity, int minimumStock) {
        return productService.create(new ProductRequest(
                "Producto " + token, "SKU-" + token + "-" + System.nanoTime(), "desc", "cat",
                new BigDecimal("10.00"), quantity, minimumStock, ProductStatus.ACTIVE));
    }

    @Test
    void conTokenValido_conectaYRecibeLaAlertaAlCruzarElUmbral() throws Exception {
        Buzon buzon = new Buzon();
        WebSocketSession session = conectar(buzon, "bearer", tokenFor("user1", "user1"));

        // El navegador cierra la conexion si el servidor no confirma el subprotocolo.
        assertThat(session.getAcceptedProtocol()).isEqualTo("bearer");
        assertThat(session.isOpen()).isTrue();

        ProductResponse producto = crearProducto("ws-alerta", 10, 5);
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "venta"));

        String mensaje = buzon.esperarNotificacion();

        assertThat(mensaje).isNotNull();
        assertThat(mensaje).contains(
                "\"type\":\"NOTIFICATION\"",
                "\"type\":\"LOW_STOCK\"",
                "\"productSku\":\"" + producto.sku() + "\"",
                "\"quantity\":2",
                "\"minimumStock\":5",
                "\"read\":false");
    }

    @Test
    void conTokenValido_noRecibeNadaSiElMovimientoNoCruzaElUmbral() throws Exception {
        Buzon buzon = new Buzon();
        conectar(buzon, "bearer", tokenFor("user1", "user1"));

        ProductResponse producto = crearProducto("ws-silencio", 50, 5);
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 3, "venta"));

        assertThat(buzon.mensajes.poll(2, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void conTokenInvalido_rechazaElHandshake() {
        assertThatThrownBy(() -> conectar(new Buzon(), "bearer", "token-invalido"))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void sinSubprotocoloBearer_rechazaElHandshake() {
        assertThatThrownBy(() -> conectar(new Buzon(), "bearer"))
                .isInstanceOf(ExecutionException.class);
    }

    @Test
    void conUsuarioSinProductView_rechazaElHandshake() {
        assertThatThrownBy(() -> conectar(new Buzon(), "bearer", tokenFor("user2", "user2")))
                .isInstanceOf(ExecutionException.class);
    }
}

package pucmm.freddy.fullstacktesting.security;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ApiResponseList;
import org.zaproxy.clientapi.core.ApiResponseSet;
import org.zaproxy.clientapi.core.ClientApi;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
@Tag("zap")
class ZapPassiveScanIT extends AbstractSecurityTest {

    private static final String IMAGEN_ZAP = "zaproxy/zap-stable:2.17.0";
    private static final int PUERTO_ZAP = 8090;
    private static final String API_KEY = "fullstacktesting-zap";
    private static final Set<String> RIESGOS_BLOQUEANTES = Set.of("High", "Medium");
    private static final Path REPORTE = Path.of("build/reports/security/zap.html");
    private static final String LINEA_BASE = "security/zap-baseline.properties";

    private static final List<String> RECORRIDO = List.of(
            "/v3/api-docs",
            "/swagger-ui/index.html",
            "/actuator/health",
            "/api/auth/me",
            "/api/products",
            "/api/stock-movements",
            "/api/notifications",
            "/api/reports/summary",
            "/api/reports/top-products",
            "/api/reports/low-stock",
            "/api/audit/products");

    private record Alerta(String pluginId, String nombre, String riesgo, String confianza, String url) {
        @Override
        public String toString() {
            return "[%s/%s] %s (%s) -> %s".formatted(riesgo, confianza, nombre, pluginId, url);
        }
    }

    @Test
    void escaneoPasivoSobreTraficoAutenticado() throws Exception {
        Testcontainers.exposeHostPorts(port);

        try (GenericContainer<?> zap = nuevoContenedorZap()) {
            zap.start();
            ClientApi api = new ClientApi(zap.getHost(), zap.getMappedPort(PUERTO_ZAP), API_KEY);
            String objetivo = "http://host.testcontainers.internal:" + port;
            String token = tokenFor("admin", "admin");

            inyectarTokenEnCadaPeticion(api, token);
            List<Integer> respuestas = recorrerEndpoints(zap, objetivo, token);
            importarOpenApi(api, objetivo);
            esperarColaPasiva(api);

            // Un escaneo que no vio trafico tambien reporta cero alertas: sin estas
            // dos comprobaciones el test pasaria por vacio en vez de por limpio.
            List<String> urls = urlsVistas(api, objetivo);
            System.out.println("ZAP recorrido: statuses=" + respuestas);
            System.out.println("ZAP urls registradas (" + urls.size() + "): " + urls);

            assertThat(respuestas)
                    .as("la app debe responder a traves del proxy de ZAP (statuses: %s)", respuestas)
                    .contains(200);
            assertThat(urls)
                    .as("ZAP no registro peticiones al objetivo")
                    .hasSizeGreaterThan(1);
            assertThat(reglasPasivasCargadas(api))
                    .as("sin reglas pasivas activas, cero alertas no significa nada")
                    .isGreaterThan(20);

            List<Alerta> alertas = alertasDe(api, objetivo);
            guardarReporte(api, zap);
            resumir(alertas);
            Properties aceptadas = lineaBase();

            List<Alerta> bloqueantes = alertas.stream()
                    .filter(alerta -> RIESGOS_BLOQUEANTES.contains(alerta.riesgo()))
                    .filter(alerta -> !aceptadas.containsKey(alerta.pluginId()))
                    .toList();

            assertThat(bloqueantes)
                    .as("alertas ZAP de riesgo alto/medio fuera de la linea base (%s)", LINEA_BASE)
                    .isEmpty();
        }
    }

    // El puerto se publica 1:1 a proposito: ZAP solo atiende su API cuando el
    // header Host coincide con su propia direccion. Con el puerto aleatorio que
    // asigna Testcontainers, ZAP interpreta la llamada como trafico a proxear y
    // responde 502.
    private GenericContainer<?> nuevoContenedorZap() {
        return new GenericContainer<>(IMAGEN_ZAP)
                .withExposedPorts(PUERTO_ZAP)
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                        .withPortBindings(new PortBinding(
                                Ports.Binding.bindPort(PUERTO_ZAP),
                                new ExposedPort(PUERTO_ZAP))))
                .withCommand("zap.sh", "-daemon",
                        "-host", "0.0.0.0",
                        "-port", String.valueOf(PUERTO_ZAP),
                        "-config", "api.key=" + API_KEY,
                        "-config", "api.addrs.addr.name=.*",
                        "-config", "api.addrs.addr.regex=true")
                .waitingFor(Wait.forHttp("/JSON/core/view/version/?apikey=" + API_KEY)
                        .forPort(PUERTO_ZAP)
                        .forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(5));
    }

    private void inyectarTokenEnCadaPeticion(ClientApi api, String token) throws Exception {
        Map<String, String> regla = new LinkedHashMap<>();
        regla.put("description", "authorization-admin");
        regla.put("enabled", "true");
        regla.put("matchType", "REQ_HEADER");
        regla.put("matchRegex", "false");
        regla.put("matchString", "Authorization");
        regla.put("replacement", "Bearer " + token);
        regla.put("initiators", "");
        api.callApi("replacer", "action", "addRule", regla);
    }

    private int reglasPasivasCargadas(ClientApi api) throws Exception {
        ApiResponse respuesta = api.callApi("pscan", "view", "scanners", Map.of());
        return respuesta instanceof ApiResponseList lista ? lista.getItems().size() : 0;
    }

    private List<String> urlsVistas(ClientApi api, String objetivo) throws Exception {
        ApiResponse respuesta = api.core.urls(objetivo);
        if (respuesta instanceof ApiResponseList lista) {
            return lista.getItems().stream()
                    .map(item -> ((ApiResponseElement) item).getValue())
                    .toList();
        }
        return List.of();
    }

    private List<Integer> recorrerEndpoints(GenericContainer<?> zap, String objetivo, String token) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setProxy(new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(zap.getHost(), zap.getMappedPort(PUERTO_ZAP))));
        RestClient viaZap = RestClient.builder()
                .requestFactory(fabrica)
                .baseUrl(objetivo)
                .build();

        return RECORRIDO.stream()
                .map(path -> viaZap.get().uri(path)
                        .header("Authorization", "Bearer " + token)
                        .exchange((request, response) -> response.getStatusCode().value()))
                .toList();
    }

    private void importarOpenApi(ClientApi api, String objetivo) throws Exception {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("url", objetivo + "/v3/api-docs");
        parametros.put("hostOverride", objetivo);
        api.callApi("openapi", "action", "importUrl", parametros);
    }

    private void esperarColaPasiva(ClientApi api) throws Exception {
        Instant limite = Instant.now().plus(Duration.ofMinutes(3));
        while (Instant.now().isBefore(limite)) {
            String pendientes = ((ApiResponseElement) api.pscan.recordsToScan()).getValue();
            if ("0".equals(pendientes)) {
                return;
            }
            Thread.sleep(1_000);
        }
        throw new IllegalStateException("ZAP no termino de procesar la cola del escaneo pasivo");
    }

    // Se consultan las dos vistas: si el filtro por baseurl no matchea (puerto
    // aleatorio, barra final) preferimos evaluar de mas y no perder una alerta.
    private List<Alerta> alertasDe(ClientApi api, String objetivo) throws Exception {
        List<Alerta> delObjetivo = mapear(api.core.alerts(objetivo, "0", "0", ""));
        List<Alerta> todas = mapear(api.core.alerts("", "0", "0", ""));
        System.out.println("ZAP alertas: " + delObjetivo.size() + " filtradas por objetivo, "
                + todas.size() + " en total");
        return delObjetivo.size() >= todas.size() ? delObjetivo : todas;
    }

    private List<Alerta> mapear(ApiResponse respuesta) {
        List<Alerta> alertas = new ArrayList<>();
        if (respuesta instanceof ApiResponseList lista) {
            for (ApiResponse item : lista.getItems()) {
                ApiResponseSet campos = (ApiResponseSet) item;
                alertas.add(new Alerta(
                        campos.getStringValue("pluginId"),
                        campos.getStringValue("alert"),
                        campos.getStringValue("risk"),
                        campos.getStringValue("confidence"),
                        campos.getStringValue("url")));
            }
        }
        return alertas;
    }

    // core.htmlreport() es la API legacy y devuelve una plantilla vacia: desde
    // ZAP 2.16 el reporte real lo produce el add-on 'reports' dentro del contenedor.
    private void guardarReporte(ClientApi api, GenericContainer<?> zap) throws Exception {
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("title", "fullstacktesting - escaneo pasivo");
        parametros.put("template", "traditional-html");
        parametros.put("reportDir", "/home/zap");
        parametros.put("reportFileName", "zap.html");
        api.callApi("reports", "action", "generate", parametros);

        Files.createDirectories(REPORTE.getParent());
        zap.copyFileFromContainer("/home/zap/zap.html", REPORTE.toString());
    }

    private void resumir(List<Alerta> alertas) {
        Map<String, Long> porRiesgo = alertas.stream()
                .collect(Collectors.groupingBy(Alerta::riesgo, TreeMap::new, Collectors.counting()));
        System.out.println("ZAP: " + alertas.size() + " alertas " + porRiesgo);
        alertas.stream().map(Alerta::toString).distinct().sorted().forEach(a -> System.out.println("  " + a));
    }

    private Properties lineaBase() throws IOException {
        Properties aceptadas = new Properties();
        try (InputStream entrada = getClass().getClassLoader().getResourceAsStream(LINEA_BASE)) {
            if (entrada != null) {
                aceptadas.load(entrada);
            }
        }
        return aceptadas;
    }
}

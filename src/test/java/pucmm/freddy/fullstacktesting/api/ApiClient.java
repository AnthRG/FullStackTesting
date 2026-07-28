package pucmm.freddy.fullstacktesting.api;

import io.restassured.RestAssured;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Arma las peticiones de RestAssured contra la aplicacion levantada por el test.
 *
 * <p>El puerto es aleatorio en cada corrida ({@code webEnvironment = RANDOM_PORT}) y se lee
 * del Environment <b>en cada peticion</b>, no al construir el bean: Spring publica
 * {@code local.server.port} cuando arranca el servidor web, que es despues de crear los
 * beans. Inyectarlo con {@code @LocalServerPort} aqui falla con "Could not resolve
 * placeholder".
 */
@Component
public class ApiClient {

    private final ScenarioContext context;
    private final Environment environment;

    public ApiClient(ScenarioContext context, Environment environment) {
        this.context = context;
        this.environment = environment;
    }

    /** Peticion con el token del escenario, si el escenario autentico a alguien. */
    public RequestSpecification autenticada() {
        RequestSpecification spec = base();
        if (context.getToken() != null) {
            spec.auth().oauth2(context.getToken());
        }
        return spec;
    }

    /** Peticion deliberadamente sin cabecera Authorization, para los casos de 401. */
    public RequestSpecification anonima() {
        return base();
    }

    /**
     * Peticion como admin, sin tocar el token del escenario. Es para el montaje de datos:
     * un escenario que prueba que user1 NO puede crear productos igual necesita que exista
     * un producto, y ese montaje no debe hacerse con el usuario bajo prueba.
     */
    public RequestSpecification comoAdmin() {
        return base().auth().oauth2(CucumberSpringConfiguration.token("admin", "admin"));
    }

    private RequestSpecification base() {
        return RestAssured.given()
                .baseUri("http://localhost")
                .port(environment.getRequiredProperty("local.server.port", Integer.class))
                .contentType(ContentType.JSON)
                // Solo imprime la peticion y la respuesta cuando el assert falla: deja el log
                // limpio en verde y da el detalle completo cuando hay que investigar.
                .log().ifValidationFails(LogDetail.ALL);
    }
}

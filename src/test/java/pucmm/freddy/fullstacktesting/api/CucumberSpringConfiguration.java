package pucmm.freddy.fullstacktesting.api;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;

/**
 * Puente entre Cucumber y Spring: le dice a Cucumber que arranque la aplicacion completa
 * en un puerto aleatorio y que los steps son beans de ese contexto.
 *
 * <p>Hereda de {@link AbstractIntegrationTest} a proposito. Ahi viven los containers de
 * PostgreSQL y Keycloak, que son estaticos y con reuse: heredar significa engancharse a
 * los mismos containers que usan los ITs en vez de levantar otro par. Tambien hereda el
 * {@code @DynamicPropertySource} que apunta el datasource y el issuer del JWT a esos
 * containers, y el helper {@code tokenFor(...)}.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class CucumberSpringConfiguration extends AbstractIntegrationTest {

    /**
     * {@code tokenFor} es protected static, asi que solo las subclases lo alcanzan.
     * Los steps no heredan de nada, por eso se expone aqui.
     */
    public static String token(String username, String password) {
        return tokenFor(username, password);
    }
}

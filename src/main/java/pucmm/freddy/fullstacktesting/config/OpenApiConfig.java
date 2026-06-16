package pucmm.freddy.fullstacktesting.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Configuracion de OpenAPI / Swagger.
 *
 * <p>Define la metadata del API y deja registrado el esquema de seguridad
 * {@code bearer-jwt} (token de Keycloak). Asi, cuando los endpoints se protejan
 * con el Resource Server, el boton <b>Authorize</b> de Swagger UI ya estara
 * disponible para pegar un access token.</p>
 *
 * <ul>
 *   <li>Swagger UI: {@code /swagger-ui.html}</li>
 *   <li>OpenAPI JSON: {@code /v3/api-docs}</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("FullStackTesting API")
                        .version("v1")
                        .description("Backend del proyecto. Incluye la administracion de "
                                + "roles de usuarios contra Keycloak."))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token (JWT) emitido por Keycloak")));
    }
}

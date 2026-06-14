package pucmm.freddy.fullstacktesting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Datos de conexion del backend a la Admin REST API de Keycloak.
 *
 * <p>Por ahora el backend se autentica con el grant {@code password} contra el
 * realm de administracion (por defecto {@code master}) usando el cliente
 * integrado {@code admin-cli}. Es lo mas simple para desarrollo. En produccion
 * conviene cambiarlo por una <b>cuenta de servicio</b> (client_credentials) con
 * permisos minimos del cliente {@code realm-management}: {@code view-users} y
 * {@code manage-users}.</p>
 *
 * <p>Prefijo de propiedades: {@code keycloak.admin}.</p>
 *
 * @param serverUrl   URL base de Keycloak, sin {@code /realms} (ej. {@code http://keycloak:8080})
 * @param authRealm   realm contra el que se autentica el backend (ej. {@code master})
 * @param targetRealm realm que se administra (ej. {@code fullstacktesting})
 * @param clientId    cliente usado para obtener el token (ej. {@code admin-cli})
 * @param username    usuario administrador
 * @param password    contrasena del administrador
 */
@ConfigurationProperties(prefix = "keycloak.admin")
public record KeycloakAdminProperties(
        String serverUrl,
        String authRealm,
        String targetRealm,
        String clientId,
        String username,
        String password) {
}

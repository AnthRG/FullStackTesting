package pucmm.freddy.fullstacktesting.api;

import io.cucumber.java.es.Dado;

/**
 * Resuelve quien hace la peticion. El token se pide a un Keycloak real levantado por
 * Testcontainers, con el realm del proyecto importado: los permisos que trae el JWT son
 * los mismos que en produccion, no un token fabricado.
 */
public class AutenticacionSteps {

    private final ScenarioContext context;

    public AutenticacionSteps(ScenarioContext context) {
        this.context = context;
    }

    @Dado("que estoy autenticado como {string}")
    public void queEstoyAutenticadoComo(String usuario) {
        // En el realm de pruebas la contrasena es igual al usuario.
        context.setToken(CucumberSpringConfiguration.token(usuario, usuario));
    }

    @Dado("que no estoy autenticado")
    public void queNoEstoyAutenticado() {
        context.setToken(null);
    }
}

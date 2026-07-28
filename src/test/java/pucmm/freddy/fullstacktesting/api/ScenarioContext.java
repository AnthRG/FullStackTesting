package pucmm.freddy.fullstacktesting.api;

import io.cucumber.spring.ScenarioScope;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

/**
 * Estado que los steps se pasan entre si dentro de un mismo escenario: la ultima
 * respuesta HTTP, el token en uso y el producto que se acaba de crear.
 *
 * <p>{@code @ScenarioScope} crea una instancia nueva por escenario. Sin esa anotacion
 * el bean seria singleton y un escenario veria la respuesta del anterior, que es la
 * fuente clasica de tests que pasan solos pero fallan en conjunto.
 */
@Component
@ScenarioScope
public class ScenarioContext {

    private Response response;
    private String token;
    private Long productId;
    private Integer cantidadInicial;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getCantidadInicial() {
        return cantidadInicial;
    }

    public void setCantidadInicial(Integer cantidadInicial) {
        this.cantidadInicial = cantidadInicial;
    }
}

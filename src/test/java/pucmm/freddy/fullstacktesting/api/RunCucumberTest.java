package pucmm.freddy.fullstacktesting.api;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

/**
 * Punto de entrada de los escenarios de API/contract testing.
 *
 * <p>No tiene metodos: JUnit descubre el motor de Cucumber, este lee los .feature de
 * {@code src/test/resources/features} y los empareja con los steps del paquete indicado
 * en el glue.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "pucmm.freddy.fullstacktesting.api")
class RunCucumberTest {
}

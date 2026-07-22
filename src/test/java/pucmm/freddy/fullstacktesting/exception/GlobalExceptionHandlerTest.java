package pucmm.freddy.fullstacktesting.exception;

import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_devuelve404ConElMensajeDeLaExcepcion() {
        ProductNotFoundException ex = new ProductNotFoundException(1L);

        ProblemDetail result = handler.handleNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getDetail()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleDuplicateSku_devuelve409ConElMensajeDeLaExcepcion() {
        DuplicateSkuException ex = new DuplicateSkuException("SKU-1");

        ProblemDetail result = handler.handleDuplicateSku(ex);

        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getDetail()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleMovementNotFound_devuelve404ConElMensajeDeLaExcepcion() {
        StockMovementNotFoundException ex = new StockMovementNotFoundException(5L);

        ProblemDetail result = handler.handleMovementNotFound(ex);

        assertThat(result.getStatus()).isEqualTo(404);
        assertThat(result.getDetail()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleInsufficientStock_devuelve409ConElMensajeDeLaExcepcion() {
        InsufficientStockException ex = new InsufficientStockException(1L, 3, 5);

        ProblemDetail result = handler.handleInsufficientStock(ex);

        assertThat(result.getStatus()).isEqualTo(409);
        assertThat(result.getDetail()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleInvalidDateRange_devuelve400ConElMensajeDeLaExcepcion() {
        InvalidDateRangeException ex = new InvalidDateRangeException(
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 1));

        ProblemDetail result = handler.handleInvalidDateRange(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getDetail()).isEqualTo(ex.getMessage());
    }

    @Test
    void handleValidationErrors_devuelve400ConMapaDeErroresPorCampo() throws NoSuchMethodException {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError nombreError = new FieldError("productRequest", "name", "no debe estar vacío");
        FieldError precioError = new FieldError("productRequest", "price", "debe ser positivo");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(nombreError, precioError));
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("metodoDummy", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ProblemDetail result = handler.handleValidationErrors(ex);

        assertThat(result.getStatus()).isEqualTo(400);
        assertThat(result.getDetail()).isEqualTo("Validación fallida");
        assertThat(result.getProperties()).containsEntry("errors", Map.of(
                "name", "no debe estar vacío",
                "price", "debe ser positivo"));
    }

    @SuppressWarnings("unused")
    private void metodoDummy(String arg) {
    }
}

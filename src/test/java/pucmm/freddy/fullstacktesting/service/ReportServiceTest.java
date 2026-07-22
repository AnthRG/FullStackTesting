package pucmm.freddy.fullstacktesting.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.domain.StockMovementRepository;
import pucmm.freddy.fullstacktesting.dto.InventorySummaryResponse;
import pucmm.freddy.fullstacktesting.dto.LowStockProductResponse;
import pucmm.freddy.fullstacktesting.exception.InvalidDateRangeException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private ReportService service;

    // ── summary ──────────────────────────────────────────────────────────────

    @Test
    void summary_ensamblaTodosLosCamposDesdeLosRepositorios() {
        when(productRepository.count()).thenReturn(10L);
        when(productRepository.countByStatus(ProductStatus.ACTIVE)).thenReturn(7L);
        when(productRepository.countByStatus(ProductStatus.INACTIVE)).thenReturn(3L);
        when(productRepository.sumTotalUnits()).thenReturn(500L);
        when(productRepository.sumInventoryValue()).thenReturn(new BigDecimal("1234.56"));
        when(productRepository.countCriticalProducts()).thenReturn(2L);
        when(stockMovementRepository.count()).thenReturn(42L);

        InventorySummaryResponse result = service.summary();

        assertThat(result.totalProducts()).isEqualTo(10L);
        assertThat(result.activeProducts()).isEqualTo(7L);
        assertThat(result.inactiveProducts()).isEqualTo(3L);
        assertThat(result.totalUnits()).isEqualTo(500L);
        assertThat(result.inventoryValue()).isEqualByComparingTo("1234.56");
        assertThat(result.criticalProducts()).isEqualTo(2L);
        assertThat(result.totalMovements()).isEqualTo(42L);
    }

    // ── topProducts / clamp de limit ────────────────────────────────────────

    @Test
    void topProducts_conLimitDentroDelRango_loUsaTalCual() {
        when(stockMovementRepository.findTopProductsByUnitsOut(any())).thenReturn(List.of());

        service.topProducts(20);

        verify(stockMovementRepository).findTopProductsByUnitsOut(PageRequest.of(0, 20));
    }

    @Test
    void topProducts_conLimitMenorQueUno_loAcotaAUno() {
        when(stockMovementRepository.findTopProductsByUnitsOut(any())).thenReturn(List.of());

        service.topProducts(0);

        verify(stockMovementRepository).findTopProductsByUnitsOut(PageRequest.of(0, 1));
    }

    @Test
    void topProducts_conLimitMayorQueCincuenta_loAcotaACincuenta() {
        when(stockMovementRepository.findTopProductsByUnitsOut(any())).thenReturn(List.of());

        service.topProducts(999);

        verify(stockMovementRepository).findTopProductsByUnitsOut(PageRequest.of(0, 50));
    }

    // ── lowStock ─────────────────────────────────────────────────────────────

    @Test
    void lowStock_delegaEnElRepositorio() {
        List<LowStockProductResponse> esperado = List.of(
                new LowStockProductResponse(1L, "Laptop", "SKU-1", "cat", 2, 5, 3));
        when(productRepository.findLowStock()).thenReturn(esperado);

        List<LowStockProductResponse> result = service.lowStock();

        assertThat(result).isEqualTo(esperado);
    }

    // ── movementsByType ──────────────────────────────────────────────────────

    @Test
    void movementsByType_conFromMayorQueTo_lanzaInvalidDateRangeException() {
        LocalDate from = LocalDate.of(2026, 1, 10);
        LocalDate to = LocalDate.of(2026, 1, 1);

        assertThatThrownBy(() -> service.movementsByType(from, to))
                .isInstanceOf(InvalidDateRangeException.class);

        verifyNoInteractions(stockMovementRepository);
    }

    @Test
    void movementsByType_sinFechas_pasaNullsAlRepositorio() {
        when(stockMovementRepository.findMovementsByType(null, null)).thenReturn(List.of());

        service.movementsByType(null, null);

        verify(stockMovementRepository).findMovementsByType(null, null);
    }

    @Test
    void movementsByType_conRangoValido_conviertelasFechasAInicioYFinDeDia() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 5);
        when(stockMovementRepository.findMovementsByType(any(), any())).thenReturn(List.of());

        service.movementsByType(from, to);

        verify(stockMovementRepository).findMovementsByType(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 6, 0, 0));
    }

    @Test
    void movementsByType_conFromIgualATo_noLanzaExcepcion() {
        LocalDate mismaFecha = LocalDate.of(2026, 1, 1);
        when(stockMovementRepository.findMovementsByType(any(), any())).thenReturn(List.of());

        service.movementsByType(mismaFecha, mismaFecha);

        verify(stockMovementRepository).findMovementsByType(
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 2, 0, 0));
    }
}

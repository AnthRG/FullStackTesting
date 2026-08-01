package pucmm.freddy.fullstacktesting.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.StockMovement;
import pucmm.freddy.fullstacktesting.domain.StockMovementRepository;
import pucmm.freddy.fullstacktesting.dto.StockMovementRequest;
import pucmm.freddy.fullstacktesting.dto.StockMovementResponse;
import pucmm.freddy.fullstacktesting.exception.InsufficientStockException;
import pucmm.freddy.fullstacktesting.exception.ProductNotFoundException;
import pucmm.freddy.fullstacktesting.exception.StockMovementNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementRepository repository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private StockMovementService service;

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    private Product sampleProduct(Long id, int quantity) {
        Product p = new Product();
        p.setId(id);
        p.setName("Laptop");
        p.setSku("SKU-001");
        p.setQuantity(quantity);
        return p;
    }

    private StockMovement sampleMovement(Long id) {
        StockMovement m = new StockMovement();
        m.setId(id);
        m.setProduct(sampleProduct(1L, 10));
        m.setMovementType(MovementType.IN);
        m.setQuantity(5);
        m.setPreviousQuantity(5);
        m.setNewQuantity(10);
        m.setUserId("system");
        return m;
    }

    private StockMovementRequest request(Long productId, MovementType type, int quantity) {
        return new StockMovementRequest(productId, type, quantity, "obs");
    }

    // ── register: IN / OUT / ADJUSTMENT ─────────────────────────────────────

    @Test
    void register_conTipoIN_sumaAlStockYGuardaElMovimiento() {
        Product product = sampleProduct(1L, 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.IN, 5));

        assertThat(product.getQuantity()).isEqualTo(15);
        assertThat(result.previousQuantity()).isEqualTo(10);
        assertThat(result.newQuantity()).isEqualTo(15);
        assertThat(result.movementType()).isEqualTo(MovementType.IN);
    }

    @Test
    void register_conTipoOUT_restaAlStockYGuardaElMovimiento() {
        Product product = sampleProduct(1L, 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.OUT, 4));

        assertThat(product.getQuantity()).isEqualTo(6);
        assertThat(result.previousQuantity()).isEqualTo(10);
        assertThat(result.newQuantity()).isEqualTo(6);
        assertThat(result.movementType()).isEqualTo(MovementType.OUT);
    }

    @Test
    void register_conTipoADJUSTMENT_fijaLaCantidadExacta() {
        Product product = sampleProduct(1L, 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.ADJUSTMENT, 3));

        assertThat(product.getQuantity()).isEqualTo(3);
        assertThat(result.previousQuantity()).isEqualTo(10);
        assertThat(result.newQuantity()).isEqualTo(3);
        assertThat(result.movementType()).isEqualTo(MovementType.ADJUSTMENT);
    }

    @Test
    void register_conOUTyStockInsuficiente_lanzaInsufficientStockException() {
        Product product = sampleProduct(1L, 3);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.register(request(1L, MovementType.OUT, 5)))
                .isInstanceOf(InsufficientStockException.class);

        verify(repository, never()).save(any());
    }

    // ── register: la resta que ve el usuario en la pantalla de productos ─────

    @Test
    void register_conSeisUnidadesYSalidaDeCuatro_dejaDos() {
        Product product = sampleProduct(1L, 6);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.OUT, 4));

        assertThat(product.getQuantity()).isEqualTo(2);
        assertThat(result.previousQuantity()).isEqualTo(6);
        assertThat(result.quantity()).isEqualTo(4);
        assertThat(result.newQuantity()).isEqualTo(2);
    }

    @Test
    void register_conSalidaExactamenteIgualAlStock_loDejaEnCero() {
        Product product = sampleProduct(1L, 6);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.OUT, 6));

        assertThat(product.getQuantity()).isZero();
        assertThat(result.newQuantity()).isZero();
    }

    @Test
    void register_conSalidaDeUnaUnidadMasQueElStock_noSePasaANegativo() {
        Product product = sampleProduct(1L, 6);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.register(request(1L, MovementType.OUT, 7)))
                .isInstanceOf(InsufficientStockException.class);

        // El stock del producto no se toca cuando el movimiento se rechaza: si quedara
        // en -1 en memoria, la transaccion podria arrastrarlo a la base.
        assertThat(product.getQuantity()).isEqualTo(6);
        verify(repository, never()).save(any());
        verify(notificationService, never()).evaluateStock(any(), anyInt(), anyInt());
    }

    @Test
    void register_conSalidaSobreStockCero_noSePasaANegativo() {
        Product product = sampleProduct(1L, 0);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.register(request(1L, MovementType.OUT, 1)))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(product.getQuantity()).isZero();
        verify(repository, never()).save(any());
    }

    @Test
    void register_conVariasSalidasSeguidas_vaDescontandoDesdeElStockVigente() {
        Product product = sampleProduct(1L, 6);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(request(1L, MovementType.OUT, 2));
        StockMovementResponse segunda = service.register(request(1L, MovementType.OUT, 3));

        assertThat(segunda.previousQuantity()).isEqualTo(4);
        assertThat(segunda.newQuantity()).isEqualTo(1);
        assertThat(product.getQuantity()).isEqualTo(1);
    }

    @Test
    void register_conProductoInexistente_lanzaProductNotFoundException() {
        when(productRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register(request(99L, MovementType.IN, 5)))
                .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).save(any());
    }

    // ── register: currentUser ────────────────────────────────────────────────

    @Test
    void register_sinAutenticacion_asignaUserIdSystem() {
        SecurityContextHolder.clearContext();
        Product product = sampleProduct(1L, 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.IN, 1));

        assertThat(result.userId()).isEqualTo("system");
    }

    @Test
    void register_conAutenticacionSinNombre_asignaUserIdSystem() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(null);
        SecurityContextHolder.getContext().setAuthentication(auth);
        Product product = sampleProduct(1L, 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.IN, 1));

        assertThat(result.userId()).isEqualTo("system");
    }

    @Test
    void register_conUsuarioAutenticado_asignaSuNombreComoUserId() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("freddy");
        SecurityContextHolder.getContext().setAuthentication(auth);
        Product product = sampleProduct(1L, 10);
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(repository.save(any(StockMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponse result = service.register(request(1L, MovementType.IN, 1));

        assertThat(result.userId()).isEqualTo("freddy");
    }

    // ── findById ─────────────────────────────────────────────────────────────

    @Test
    void findById_conIdExistente_retornaElMovimiento() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleMovement(1L)));

        StockMovementResponse result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.movementType()).isEqualTo(MovementType.IN);
    }

    @Test
    void findById_conIdInexistente_lanzaStockMovementNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(StockMovementNotFoundException.class);
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_sinFiltros_retornaPaginaDeMovimientos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockMovement> page = new PageImpl<>(List.of(sampleMovement(1L)));
        when(repository.findAll(ArgumentMatchers.<Specification<StockMovement>>any(), eq(pageable))).thenReturn(page);

        Page<StockMovementResponse> result = service.list(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void list_conProductId_filtraPorProducto() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockMovement> page = new PageImpl<>(List.of(sampleMovement(1L)));
        when(repository.findAll(ArgumentMatchers.<Specification<StockMovement>>any(), eq(pageable))).thenReturn(page);

        Page<StockMovementResponse> result = service.list(1L, null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void list_conMovementType_filtraPorTipo() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockMovement> page = new PageImpl<>(List.of(sampleMovement(1L)));
        when(repository.findAll(ArgumentMatchers.<Specification<StockMovement>>any(), eq(pageable))).thenReturn(page);

        Page<StockMovementResponse> result = service.list(null, MovementType.IN, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void list_conProductIdYMovementType_combinaAmbosFiltros() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<StockMovement> page = new PageImpl<>(List.of(sampleMovement(1L)));
        when(repository.findAll(ArgumentMatchers.<Specification<StockMovement>>any(), eq(pageable))).thenReturn(page);

        Page<StockMovementResponse> result = service.list(1L, MovementType.OUT, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void list_sinResultados_retornaPaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(ArgumentMatchers.<Specification<StockMovement>>any(), eq(pageable))).thenReturn(Page.empty());

        Page<StockMovementResponse> result = service.list(1L, MovementType.ADJUSTMENT, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}

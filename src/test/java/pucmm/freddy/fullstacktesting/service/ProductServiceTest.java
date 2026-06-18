package pucmm.freddy.fullstacktesting.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.exception.DuplicateSkuException;
import pucmm.freddy.fullstacktesting.exception.ProductNotFoundException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    @InjectMocks
    private ProductService service;

    private ProductRequest sampleRequest() {
        return new ProductRequest("Laptop", "SKU-001", "Laptop gamer", "Electrónica",
                new BigDecimal("999.99"), 10, 2, ProductStatus.ACTIVE);
    }

    private Product sampleProduct(Long id) {
        Product p = new Product();
        p.setId(id);
        p.setName("Laptop");
        p.setSku("SKU-001");
        p.setDescription("Laptop gamer");
        p.setCategory("Electrónica");
        p.setPrice(new BigDecimal("999.99"));
        p.setQuantity(10);
        p.setMinimumStock(2);
        p.setStatus(ProductStatus.ACTIVE);
        return p;
    }

    @Test
    void create_conSkuNuevo_creaElProducto() {
        ProductRequest req = sampleRequest();
        when(repository.existsBySku(req.sku())).thenReturn(false);
        when(repository.save(any(Product.class))).thenReturn(sampleProduct(1L));

        ProductResponse result = service.create(req);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.sku()).isEqualTo("SKU-001");
        verify(repository).save(any(Product.class));
    }

    @Test
    void create_conSkuExistente_lanzaDuplicateSkuException() {
        ProductRequest req = sampleRequest();
        when(repository.existsBySku(req.sku())).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateSkuException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_conDatosValidos_actualizaElProducto() {
        Long id = 1L;
        ProductRequest req = sampleRequest();
        Product existing = sampleProduct(id);
        when(repository.existsBySkuAndIdNot(req.sku(), id)).thenReturn(false);
        when(repository.findById(id)).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any(Product.class))).thenReturn(existing);

        ProductResponse result = service.update(id, req);

        assertThat(result.name()).isEqualTo("Laptop");
        verify(repository).save(existing);
    }

    @Test
    void update_conSkuDeOtroProducto_lanzaDuplicateSkuException() {
        Long id = 1L;
        ProductRequest req = sampleRequest();
        when(repository.existsBySkuAndIdNot(req.sku(), id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(DuplicateSkuException.class);

        verify(repository, never()).findById(any());
    }

    @Test
    void update_conIdInexistente_lanzaProductNotFoundException() {
        Long id = 99L;
        ProductRequest req = sampleRequest();
        when(repository.existsBySkuAndIdNot(req.sku(), id)).thenReturn(false);
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void findById_conIdExistente_retornaProducto() {
        when(repository.findById(1L)).thenReturn(Optional.of(sampleProduct(1L)));

        ProductResponse result = service.findById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Laptop");
    }

    @Test
    void findById_conIdInexistente_lanzaProductNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_conIdExistente_eliminaElProducto() {
        when(repository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void delete_conIdInexistente_lanzaProductNotFoundException() {
        when(repository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(ProductNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    void list_sinFiltros_retornaPaginaDeProductos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct(1L)));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = service.list(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).sku()).isEqualTo("SKU-001");
    }

    @Test
    void list_conSearch_retornaSoloCoincidencias() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct(1L)));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = service.list("Laptop", null, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void list_conStatus_retornaSoloEseStatus() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct(1L)));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = service.list(null, ProductStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    void list_conSearchYStatus_combinaAmbosFiltos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(sampleProduct(1L)));
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProductResponse> result = service.list("Laptop", ProductStatus.ACTIVE, pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void list_sinResultados_retornaPaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(Page.empty());

        Page<ProductResponse> result = service.list("XYZ", ProductStatus.INACTIVE, pageable);

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    // ── create / mapeo de campos ───────────────────────────────────────────────

    @Test
    void create_mapeaCorrectamenteTodosLosCampos() {
        ProductRequest req = sampleRequest();
        when(repository.existsBySku(req.sku())).thenReturn(false);
        when(repository.save(any(Product.class))).thenReturn(sampleProduct(1L));

        ProductResponse result = service.create(req);

        assertThat(result.name()).isEqualTo(req.name());
        assertThat(result.price()).isEqualByComparingTo(req.price());
        assertThat(result.quantity()).isEqualTo(req.quantity());
        assertThat(result.category()).isEqualTo(req.category());
        assertThat(result.status()).isEqualTo(req.status());
    }

    // ── update / mapeo de campos ───────────────────────────────────────────────

    @Test
    void update_actualizaTodosLosCamposDelProducto() {
        Long id = 1L;
        ProductRequest req = new ProductRequest("Monitor", "SKU-002", "4K Monitor", "Periféricos",
                new BigDecimal("499.99"), 5, 1, ProductStatus.INACTIVE);
        Product existing = sampleProduct(id);
        when(repository.existsBySkuAndIdNot(req.sku(), id)).thenReturn(false);
        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse result = service.update(id, req);

        assertThat(result.name()).isEqualTo("Monitor");
        assertThat(result.sku()).isEqualTo("SKU-002");
        assertThat(result.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(result.price()).isEqualByComparingTo(new BigDecimal("499.99"));
    }
}

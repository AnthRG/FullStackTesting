package pucmm.freddy.fullstacktesting.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.exception.DuplicateSkuException;
import pucmm.freddy.fullstacktesting.exception.ProductNotFoundException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ProductService service;

    @PersistenceContext
    private EntityManager em;

    @Test
    void create_persisteYsePuedeRecuperarPorId() {
        ProductResponse creado = service.create(request("uno", "1", ProductStatus.ACTIVE));
        reload();

        ProductResponse encontrado = service.findById(creado.id());

        assertThat(encontrado.sku()).isEqualTo("SKU-uno-1");
        assertThat(encontrado.name()).isEqualTo("Producto uno");
        assertThat(encontrado.price()).isEqualByComparingTo("10.00");
    }

    @Test
    void create_conSkuDuplicado_lanzaDuplicateSkuException() {
        service.create(request("dup", "1", ProductStatus.ACTIVE));

        assertThatThrownBy(() -> service.create(request("dup", "1", ProductStatus.ACTIVE)))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void update_modificaLosCamposPersistidos() {
        ProductResponse creado = service.create(request("edit", "1", ProductStatus.ACTIVE));
        reload();

        service.update(creado.id(), new ProductRequest(
                "Editado", "SKU-edit-1", "nueva desc", "otra cat",
                new BigDecimal("55.50"), 99, 7, ProductStatus.INACTIVE));
        reload();

        ProductResponse actualizado = service.findById(creado.id());
        assertThat(actualizado.name()).isEqualTo("Editado");
        assertThat(actualizado.quantity()).isEqualTo(99);
        assertThat(actualizado.status()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(actualizado.price()).isEqualByComparingTo("55.50");
    }

    @Test
    void update_conSkuDeOtroProducto_lanzaDuplicateSkuException() {
        service.create(request("ocupado", "1", ProductStatus.ACTIVE));
        ProductResponse otro = service.create(request("libre", "2", ProductStatus.ACTIVE));
        reload();

        ProductRequest pisaElSku = new ProductRequest(
                "Producto libre", "SKU-ocupado-1", "desc", "cat",
                new BigDecimal("10.00"), 5, 1, ProductStatus.ACTIVE);

        assertThatThrownBy(() -> service.update(otro.id(), pisaElSku))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void delete_eliminaElProducto() {
        ProductResponse creado = service.create(request("borrar", "1", ProductStatus.ACTIVE));
        reload();

        service.delete(creado.id());
        reload();

        assertThatThrownBy(() -> service.findById(creado.id()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void list_buscaPorTextoIgnorandoMayusculas() {
        service.create(request("Busqueda", "1", ProductStatus.ACTIVE));
        reload();

        Page<ProductResponse> res = service.list("busqueda", null, PageRequest.of(0, 10));

        assertThat(res.getContent()).extracting(ProductResponse::sku).containsExactly("SKU-Busqueda-1");
    }

    @Test
    void list_filtraPorStatusYTexto() {
        service.create(request("filtro", "1", ProductStatus.ACTIVE));
        service.create(request("filtro", "2", ProductStatus.ACTIVE));
        service.create(request("filtro", "3", ProductStatus.INACTIVE));
        reload();

        Page<ProductResponse> activos = service.list("filtro", ProductStatus.ACTIVE, PageRequest.of(0, 10));
        Page<ProductResponse> inactivos = service.list("filtro", ProductStatus.INACTIVE, PageRequest.of(0, 10));

        assertThat(activos.getTotalElements()).isEqualTo(2);
        assertThat(inactivos.getTotalElements()).isEqualTo(1);
    }

    @Test
    void list_paginaLosResultados() {
        service.create(request("pagina", "1", ProductStatus.ACTIVE));
        service.create(request("pagina", "2", ProductStatus.ACTIVE));
        service.create(request("pagina", "3", ProductStatus.ACTIVE));
        reload();

        Page<ProductResponse> primera = service.list("pagina", null, PageRequest.of(0, 2));
        Page<ProductResponse> segunda = service.list("pagina", null, PageRequest.of(1, 2));

        assertThat(primera.getTotalElements()).isEqualTo(3);
        assertThat(primera.getTotalPages()).isEqualTo(2);
        assertThat(primera.getContent()).hasSize(2);
        assertThat(segunda.getContent()).hasSize(1);
    }

    private void reload() {
        em.flush();
        em.clear();
    }

    private ProductRequest request(String nameToken, String skuSuffix, ProductStatus status) {
        return new ProductRequest(
                "Producto " + nameToken,
                "SKU-" + nameToken + "-" + skuSuffix,
                "desc", "cat",
                new BigDecimal("10.00"), 5, 1, status);
    }
}

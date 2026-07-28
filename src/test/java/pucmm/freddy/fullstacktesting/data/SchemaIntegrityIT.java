package pucmm.freddy.fullstacktesting.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Ataca el esquema con SQL directo, saltandose JPA y Bean Validation, para comprobar que
 * la base se defiende sola: si alguien escribe por fuera de la aplicacion, las
 * restricciones de las migraciones tienen que seguir rechazando los datos invalidos.
 *
 * Sin {@code @Transactional} a proposito: una violacion de restriccion aborta la
 * transaccion en Postgres, asi que cada sentencia se ejecuta en autocommit y la limpieza
 * va en {@link #limpiarDatosDePrueba()}.
 */
@SpringBootTest
@ActiveProfiles("test")
class SchemaIntegrityIT extends AbstractIntegrationTest {

    private static final String SKU_PREFIX = "SCHEMA-";

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbc.update("DELETE FROM stock_movements WHERE product_id IN "
                + "(SELECT id FROM products WHERE sku LIKE ?)", SKU_PREFIX + "%");
        jdbc.update("DELETE FROM products WHERE sku LIKE ?", SKU_PREFIX + "%");
    }

    @Test
    void flyway_aplicoTodasLasMigracionesSinFallos() {
        List<String> fallidas = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = false", String.class);
        assertThat(fallidas).as("migraciones marcadas como fallidas").isEmpty();

        List<String> aplicadas = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE version IS NOT NULL", String.class);
        assertThat(aplicadas).contains("1", "2", "3", "4", "5");
    }

    @Test
    void precioNegativo_violaElCheck() {
        assertThatThrownBy(() -> insertarProducto(sku("precio"), "ACTIVE", "-0.01", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_price");
    }

    @Test
    void cantidadNegativa_violaElCheck() {
        assertThatThrownBy(() -> insertarProducto(sku("cantidad"), "ACTIVE", "10.00", -1, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_quantity");
    }

    @Test
    void stockMinimoNegativo_violaElCheck() {
        assertThatThrownBy(() -> insertarProducto(sku("minimo"), "ACTIVE", "10.00", 1, -1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_min_stock");
    }

    @Test
    void statusFueraDelDominio_violaElCheck() {
        assertThatThrownBy(() -> insertarProducto(sku("status"), "PENDIENTE", "10.00", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_status");
    }

    @Test
    void skuDuplicado_violaLaRestriccionDeUnicidad() {
        String sku = sku("duplicado");
        insertarProducto(sku, "ACTIVE", "10.00", 1, 0);

        assertThatThrownBy(() -> insertarProducto(sku, "ACTIVE", "10.00", 1, 0))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void skuDeMasDe50Caracteres_noCabeEnLaColumna() {
        String largo = SKU_PREFIX + "X".repeat(51);

        assertThatThrownBy(() -> insertarProducto(largo, "ACTIVE", "10.00", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void movimientoDeProductoInexistente_violaLaClaveForanea() {
        assertThatThrownBy(() -> insertarMovimiento(999_999_999L, "IN", 1, 0, 1))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_stock_movements_product");
    }

    @Test
    void borrarUnProductoConMovimientos_violaLaClaveForanea() {
        Long productId = insertarProducto(sku("referenciado"), "ACTIVE", "10.00", 5, 1);
        insertarMovimiento(productId, "IN", 3, 5, 8);

        assertThatThrownBy(() -> jdbc.update("DELETE FROM products WHERE id = ?", productId))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_stock_movements_product");
    }

    @Test
    void tipoDeMovimientoInvalido_violaElCheck() {
        Long productId = insertarProducto(sku("tipo"), "ACTIVE", "10.00", 5, 1);

        assertThatThrownBy(() -> insertarMovimiento(productId, "DEVOLUCION", 1, 5, 6))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_movement_type");
    }

    @Test
    void cantidadDeMovimientoNoPositiva_violaElCheck() {
        Long productId = insertarProducto(sku("cantidad-mov"), "ACTIVE", "10.00", 5, 1);

        assertThatThrownBy(() -> insertarMovimiento(productId, "IN", 0, 5, 5))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_movement_quantity");

        assertThatThrownBy(() -> insertarMovimiento(productId, "IN", -3, 5, 2))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_movement_quantity");
    }

    @Test
    void nuevaCantidadNegativa_violaElCheck() {
        Long productId = insertarProducto(sku("nueva-cantidad"), "ACTIVE", "10.00", 5, 1);

        assertThatThrownBy(() -> insertarMovimiento(productId, "OUT", 10, 5, -5))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_new_quantity");
    }

    @Test
    void losIndicesDeMovimientosExisten() {
        List<String> indices = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'stock_movements'", String.class);

        assertThat(indices)
                .as("indices que sostienen los filtros por producto y por fecha")
                .contains("idx_stock_movements_product", "idx_stock_movements_created_at");
    }

    @Test
    void laAuditoriaDeEnversApuntaASuTablaDeRevisiones() {
        List<String> tablas = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
                String.class);

        assertThat(tablas).contains("revinfo", "products_aud", "stock_movements_aud");
    }

    private String sku(String token) {
        return SKU_PREFIX + token + "-" + System.nanoTime();
    }

    private Long insertarProducto(String sku, String status, String price, int quantity, int minimumStock) {
        return jdbc.queryForObject("""
                INSERT INTO products (name, sku, description, category, price, quantity, minimum_stock, status)
                VALUES (?, ?, 'creado por el test de esquema', 'datos', ?, ?, ?, ?)
                RETURNING id
                """, Long.class, "Producto de esquema", sku, new BigDecimal(price), quantity, minimumStock, status);
    }

    private void insertarMovimiento(Long productId, String type, int quantity, int previous, int updated) {
        jdbc.update("""
                INSERT INTO stock_movements
                    (product_id, movement_type, quantity, previous_quantity, new_quantity, user_id)
                VALUES (?, ?, ?, ?, ?, 'test')
                """, productId, type, quantity, previous, updated);
    }
}

-- Datos de demostracion del inventario.
--
-- Migracion REPETIBLE (prefijo R__): Flyway la aplica despues de las versionadas y la vuelve
-- a ejecutar cuando cambia su contenido. Por eso todo aqui es idempotente: si los datos ya
-- existen, no se duplican.
--
-- Solo se carga en el perfil 'dev' (ver spring.flyway.locations en application-dev.properties):
-- produccion nunca ve estos datos.
--
-- Los productos cubren a proposito los cuatro estados que interesan al probar:
--   stock sano, stock critico (cantidad <= minimo), sin stock (cantidad = 0) e inactivo.
-- Los movimientos cuadran con la cantidad de cada producto: la suma algebraica de entradas y
-- salidas es exactamente el stock actual, y cada movimiento encadena la cantidad anterior con
-- la nueva. Sin eso, los datos sembrados romperian la conciliacion del inventario.

INSERT INTO products (name, sku, description, category, price, quantity, minimum_stock, status,
                      created_by, updated_by)
VALUES
    ('Laptop Pro 14',        'SEED-LAP-001', 'Portatil 14 pulgadas, 16 GB RAM, 512 GB SSD', 'Computo',        1250.00,  12,  3, 'ACTIVE',   'seed', 'seed'),
    ('Monitor 27 4K',        'SEED-MON-002', 'Monitor IPS 27 pulgadas, 3840x2160',          'Perifericos',     389.99,   8,  4, 'ACTIVE',   'seed', 'seed'),
    ('Teclado mecanico',     'SEED-TEC-003', 'Teclado mecanico retroiluminado, switch rojo', 'Perifericos',      89.50,   2,  5, 'ACTIVE',   'seed', 'seed'),
    ('Mouse inalambrico',    'SEED-MOU-004', 'Mouse optico inalambrico 2.4 GHz',            'Perifericos',      24.90,   0,  6, 'ACTIVE',   'seed', 'seed'),
    ('SSD NVMe 1 TB',        'SEED-SSD-005', 'Unidad de estado solido NVMe Gen4',           'Almacenamiento',   99.00,  45, 10, 'ACTIVE',   'seed', 'seed'),
    ('Memoria RAM 16 GB',    'SEED-RAM-006', 'Modulo DDR4 3200 MHz',                        'Componentes',      64.75,  30,  8, 'ACTIVE',   'seed', 'seed'),
    ('Impresora laser',      'SEED-IMP-007', 'Impresora laser monocromatica de red',        'Oficina',         210.00,   5,  2, 'ACTIVE',   'seed', 'seed'),
    ('Cable HDMI 2 m',       'SEED-CAB-008', 'Cable HDMI 2.1 de 2 metros',                  'Accesorios',        7.99, 120, 25, 'ACTIVE',   'seed', 'seed'),
    ('Silla ergonomica',     'SEED-SIL-009', 'Silla de oficina con soporte lumbar',         'Mobiliario',      275.00,   6,  2, 'INACTIVE', 'seed', 'seed'),
    ('UPS 1500 VA',          'SEED-UPS-010', 'Respaldo de energia 1500 VA, 900 W',          'Energia',         180.00,   3,  3, 'ACTIVE',   'seed', 'seed')
ON CONFLICT (sku) DO NOTHING;

-- Historial de movimientos. Se siembra una sola vez por producto: si ya hay movimientos
-- sembrados para ese producto, no se vuelve a insertar nada.
INSERT INTO stock_movements (product_id, movement_type, quantity, previous_quantity, new_quantity,
                             user_id, observations, created_at)
SELECT p.id, m.movement_type, m.quantity, m.previous_quantity, m.new_quantity,
       'seed', m.observations, NOW() - (m.dias_atras * INTERVAL '1 day')
FROM (VALUES
    ('SEED-LAP-001', 'IN',         20,   0,  20, 'Compra inicial del trimestre',   30),
    ('SEED-LAP-001', 'OUT',         5,  20,  15, 'Asignacion a ventas',            18),
    ('SEED-LAP-001', 'OUT',         3,  15,  12, 'Asignacion a soporte',            6),
    ('SEED-MON-002', 'IN',         15,   0,  15, 'Compra inicial',                 28),
    ('SEED-MON-002', 'OUT',         7,  15,   8, 'Montaje de estaciones nuevas',    9),
    ('SEED-SSD-005', 'IN',         50,   0,  50, 'Compra por volumen',             25),
    ('SEED-SSD-005', 'OUT',         5,  50,  45, 'Ampliacion de equipos',           4),
    ('SEED-CAB-008', 'IN',        150,   0, 150, 'Compra por volumen',             22),
    ('SEED-CAB-008', 'OUT',        30, 150, 120, 'Consumo de sala de reuniones',    3),
    ('SEED-IMP-007', 'ADJUSTMENT',  5,   0,   5, 'Ajuste por conteo fisico',       12)
) AS m (sku, movement_type, quantity, previous_quantity, new_quantity, observations, dias_atras)
JOIN products p ON p.sku = m.sku
WHERE NOT EXISTS (
    SELECT 1 FROM stock_movements s WHERE s.product_id = p.id AND s.user_id = 'seed'
);

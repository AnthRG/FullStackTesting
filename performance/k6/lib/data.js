// Generadores de payloads. El SKU tiene restriccion UNIQUE en la base, asi que cada
// peticion necesita uno propio: __VU y __ITER identifican al usuario virtual y su
// iteracion, lo que evita colisiones aunque corran cientos en paralelo.

// __VU y __ITER solo existen dentro del contexto de un usuario virtual: en setup() y
// teardown() no estan definidos, asi que hay que protegerlos.
export function uniqueSku(prefix = 'K6') {
  const vu = typeof __VU === 'undefined' ? 0 : __VU;
  const iter = typeof __ITER === 'undefined' ? 0 : __ITER;
  return `${prefix}-${vu}-${iter}-${Date.now()}`;
}

export function productPayload(overrides = {}) {
  return Object.assign(
    {
      name: 'Producto de carga k6',
      sku: uniqueSku(),
      description: 'Creado por la suite de performance',
      category: 'Performance',
      price: 19.99,
      quantity: 100,
      minimumStock: 5,
      status: 'ACTIVE',
    },
    overrides,
  );
}

export function movementPayload(productId, movementType = 'IN', quantity = 1) {
  return {
    productId,
    movementType,
    quantity,
    observations: 'Movimiento generado por la suite de performance',
  };
}

// Reparte las lecturas entre varias paginas para que la base no responda siempre
// desde el mismo plan cacheado y el numero se parezca mas al uso real.
export function randomPage(totalPages = 5) {
  return Math.floor(Math.random() * totalPages);
}

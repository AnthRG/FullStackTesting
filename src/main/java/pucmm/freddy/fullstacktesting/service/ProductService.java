package pucmm.freddy.fullstacktesting.service;


import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.exception.DuplicateSkuException;
import pucmm.freddy.fullstacktesting.exception.ProductNotFoundException;

@Service
@RequiredArgsConstructor

public class ProductService {

    private final ProductRepository repository;
    private final NotificationService notificationService;


    public Page<ProductResponse> list(String search, ProductStatus status, Pageable pageable) {
        Specification<Product> spec = Specification.unrestricted();

        if (search != null && !search.isBlank()) {
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), "%" + search.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("sku")), "%" + search.toLowerCase() + "%")
            ));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        return repository.findAll(spec, pageable).map(ProductResponse::from);
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.from(getOrThrow(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest req) {
        String sku = req.sku().trim().toUpperCase();
        if (repository.existsBySku(sku)) throw new DuplicateSkuException(sku);
        Product saved;
        try {
            saved = repository.save(toEntity(req));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSkuException(sku);
        }
        // Sin estado previo: un producto que nace bajo minimo tambien alerta.
        notificationService.evaluateStock(saved, null, null);
        return ProductResponse.from(saved);
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        String sku = req.sku().trim().toUpperCase();
        if (repository.existsBySkuAndIdNot(sku, id)) throw new DuplicateSkuException(sku);
        // Bloquea la fila como hace el registro de movimientos: dos updates concurrentes
        // leerian la misma cantidad previa y emitirian la alerta de umbral por duplicado.
        Product product = repository.findByIdForUpdate(id).orElseThrow(() -> new ProductNotFoundException(id));
        Integer previousQuantity = product.getQuantity();
        Integer previousMinimum = product.getMinimumStock();
        product.setName(req.name());
        product.setSku(sku);
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        product.setMinimumStock(req.minimumStock());
        product.setStatus(req.status());
        Product saved;
        try {
            saved = repository.save(product);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateSkuException(sku);
        }
        notificationService.evaluateStock(saved, previousQuantity, previousMinimum);
        return ProductResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) throw new ProductNotFoundException(id);
        repository.deleteById(id);
    }

    private Product getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Product toEntity(ProductRequest req) {
        Product product = new Product();
        product.setName(req.name());
        product.setSku(req.sku().trim().toUpperCase());
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        product.setMinimumStock(req.minimumStock());
        product.setStatus(req.status());
        return product;
    }
    
}

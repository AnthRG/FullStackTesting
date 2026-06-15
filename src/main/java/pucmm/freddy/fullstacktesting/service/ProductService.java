package pucmm.freddy.fullstacktesting.service;


import lombok.RequiredArgsConstructor;

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
        if (repository.existsBySku(req.sku())) throw new DuplicateSkuException(req.sku());
        return ProductResponse.from(repository.save(toEntity(req)));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest req) {
        if (repository.existsBySkuAndIdNot(req.sku(), id)) throw new DuplicateSkuException(req.sku());
        Product product = getOrThrow(id);
        product.setName(req.name());
        product.setSku(req.sku());
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        product.setMinimumStock(req.minimumStock());
        product.setStatus(req.status());
        return ProductResponse.from(repository.save(product));
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
        product.setSku(req.sku());
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setPrice(req.price());
        product.setQuantity(req.quantity());
        product.setMinimumStock(req.minimumStock());
        product.setStatus(req.status());
        return product;
    }
    
}

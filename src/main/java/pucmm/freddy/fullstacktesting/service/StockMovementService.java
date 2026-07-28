package pucmm.freddy.fullstacktesting.service;


import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

@Service
@RequiredArgsConstructor

public class StockMovementService {

    private final StockMovementRepository repository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;


    @Transactional(readOnly = true)
    public Page<StockMovementResponse> list(Long productId, MovementType movementType, Pageable pageable) {
        Specification<StockMovement> spec = Specification.unrestricted();

        if (productId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("product").get("id"), productId));
        }
        if (movementType != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("movementType"), movementType));
        }

        return repository.findAll(spec, pageable).map(StockMovementResponse::from);
    }

    @Transactional(readOnly = true)
    public StockMovementResponse findById(Long id) {
        return StockMovementResponse.from(
                repository.findById(id).orElseThrow(() -> new StockMovementNotFoundException(id)));
    }

    @Transactional
    public StockMovementResponse register(StockMovementRequest req) {
        Product product = productRepository.findByIdForUpdate(req.productId())
                .orElseThrow(() -> new ProductNotFoundException(req.productId()));

        int previous = product.getQuantity();
        int updated = switch (req.movementType()) {
            case IN -> previous + req.quantity();
            case OUT -> previous - req.quantity();
            case ADJUSTMENT -> req.quantity();
        };
        if (updated < 0) {
            throw new InsufficientStockException(product.getId(), previous, req.quantity());
        }
        product.setQuantity(updated);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(req.movementType());
        movement.setQuantity(req.quantity());
        movement.setPreviousQuantity(previous);
        movement.setNewQuantity(updated);
        movement.setUserId(currentUser());
        movement.setObservations(req.observations());
        StockMovement saved = repository.save(movement);

        // El movimiento no toca el minimo, asi que el minimo previo es el actual.
        notificationService.evaluateStock(product, previous, product.getMinimumStock());

        return StockMovementResponse.from(saved);
    }

    private String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = (auth != null) ? auth.getName() : null;
        return name != null ? name : "system";
    }

}

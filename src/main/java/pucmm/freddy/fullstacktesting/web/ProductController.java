package pucmm.freddy.fullstacktesting.web;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.service.ProductService;


@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "CRUD de productos del invetario")

public class ProductController {

    private final ProductService service;

    @GetMapping
    public Page<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return service.list(search, status, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return service.update(id, req);
    }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
    
}

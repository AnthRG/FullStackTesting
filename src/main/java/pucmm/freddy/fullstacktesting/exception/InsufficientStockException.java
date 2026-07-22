package pucmm.freddy.fullstacktesting.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, Integer available, Integer requested) {
        super("Stock insuficiente para el producto " + productId
                + ": disponible " + available + ", solicitado " + requested);
    }
}

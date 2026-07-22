package pucmm.freddy.fullstacktesting.exception;

public class StockMovementNotFoundException extends RuntimeException {
    public StockMovementNotFoundException(Long id) {
        super("Movimiento de stock no encontrado con id: " + id);
    }
}

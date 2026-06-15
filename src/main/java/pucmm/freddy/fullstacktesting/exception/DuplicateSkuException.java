package pucmm.freddy.fullstacktesting.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) {
        super("El SKU '" + sku + "' ya existe. Debe ser único.");
    }
    
}

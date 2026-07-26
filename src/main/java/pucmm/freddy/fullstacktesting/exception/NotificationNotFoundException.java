package pucmm.freddy.fullstacktesting.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException(Long id) {
        super("Notificación no encontrada con id: " + id);
    }
}

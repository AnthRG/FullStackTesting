package pucmm.freddy.fullstacktesting.exception;

import java.time.LocalDate;

public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException(LocalDate from, LocalDate to) {
        super("Rango de fechas inválido: 'from' (" + from + ") es posterior a 'to' (" + to + ")");
    }
}

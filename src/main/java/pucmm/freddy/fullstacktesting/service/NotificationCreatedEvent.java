package pucmm.freddy.fullstacktesting.service;

import pucmm.freddy.fullstacktesting.dto.NotificationResponse;

/**
 * Se publica dentro de la transacción que persiste la alerta, pero solo se consume
 * después del commit. Lleva el DTO ya armado (no la entidad) para que el listener
 * no dependa de una sesión de Hibernate ya cerrada.
 */
public record NotificationCreatedEvent(NotificationResponse payload) {
}

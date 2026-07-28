package pucmm.freddy.fullstacktesting.dto;

import java.util.List;

/** Listado de alertas más el contador global de no leídas. */
public record NotificationListResponse(

        List<NotificationResponse> items,

        long unreadCount

) {
}

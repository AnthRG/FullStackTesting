package pucmm.freddy.fullstacktesting.dto;

import java.time.LocalDateTime;

public record ProductAuditFeedItem(

        int revision,

        LocalDateTime revisionDate,

        String username,

        String revisionType,

        Long productId,

        String productName,

        String productSku

) {}

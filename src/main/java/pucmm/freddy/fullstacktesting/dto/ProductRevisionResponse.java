package pucmm.freddy.fullstacktesting.dto;

import java.time.LocalDateTime;

public record ProductRevisionResponse(

        int revision,

        LocalDateTime revisionDate,

        String username,

        String revisionType,

        ProductSnapshot product

) {}

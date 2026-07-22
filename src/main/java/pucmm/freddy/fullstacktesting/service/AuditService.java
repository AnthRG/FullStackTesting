package pucmm.freddy.fullstacktesting.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pucmm.freddy.fullstacktesting.audit.AuditRevision;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.dto.ProductAuditFeedItem;
import pucmm.freddy.fullstacktesting.dto.ProductRevisionResponse;
import pucmm.freddy.fullstacktesting.dto.ProductSnapshot;
import pucmm.freddy.fullstacktesting.exception.ProductNotFoundException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class AuditService {

    private static final int MIN_SIZE = 1;
    private static final int MAX_SIZE = 50;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<ProductRevisionResponse> productRevisions(Long id) {
        AuditReader reader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        if (rows.isEmpty()) {
            throw new ProductNotFoundException(id);
        }

        return rows.stream().map(this::toRevisionResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductAuditFeedItem> productFeed(int page, int size) {
        int safeSize = Math.min(MAX_SIZE, Math.max(MIN_SIZE, size));
        int safePage = Math.max(0, page);
        long offset = Math.min((long) safePage * safeSize, Integer.MAX_VALUE);
        AuditReader reader = AuditReaderFactory.get(entityManager);

        Number total = (Number) reader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .addProjection(AuditEntity.revisionNumber().count())
                .getSingleResult();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Product.class, false, true)
                .addOrder(AuditEntity.revisionNumber().desc())
                .setFirstResult((int) offset)
                .setMaxResults(safeSize)
                .getResultList();

        List<ProductAuditFeedItem> content = rows.stream().map(this::toFeedItem).toList();
        return new PageImpl<>(content, PageRequest.of(safePage, safeSize), total.longValue());
    }

    private ProductRevisionResponse toRevisionResponse(Object[] row) {
        Product entity = (Product) row[0];
        AuditRevision revision = (AuditRevision) row[1];
        RevisionType type = (RevisionType) row[2];
        return new ProductRevisionResponse(
                revision.getId(),
                toDateTime(revision.getTimestamp()),
                revision.getUsername(),
                revisionTypeName(type),
                entity != null ? ProductSnapshot.from(entity) : null);
    }

    private ProductAuditFeedItem toFeedItem(Object[] row) {
        Product entity = (Product) row[0];
        AuditRevision revision = (AuditRevision) row[1];
        RevisionType type = (RevisionType) row[2];
        return new ProductAuditFeedItem(
                revision.getId(),
                toDateTime(revision.getTimestamp()),
                revision.getUsername(),
                revisionTypeName(type),
                entity != null ? entity.getId() : null,
                entity != null ? entity.getName() : null,
                entity != null ? entity.getSku() : null);
    }

    private String revisionTypeName(RevisionType type) {
        return switch (type) {
            case ADD -> "CREATE";
            case MOD -> "UPDATE";
            case DEL -> "DELETE";
        };
    }

    private LocalDateTime toDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }
}

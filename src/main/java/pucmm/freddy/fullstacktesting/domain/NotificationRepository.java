package pucmm.freddy.fullstacktesting.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("select n from Notification n order by n.createdAt desc, n.id desc")
    List<Notification> findLatest(Pageable pageable);

    @Query("select n from Notification n where n.read = false order by n.createdAt desc, n.id desc")
    List<Notification> findLatestUnread(Pageable pageable);

    long countByReadFalse();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.read = true, n.readAt = :readAt where n.read = false")
    int markAllRead(@Param("readAt") LocalDateTime readAt);
}

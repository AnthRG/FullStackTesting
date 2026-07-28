package pucmm.freddy.fullstacktesting.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Alerta de stock del inventario.
 *
 * <p>Las notificaciones son <b>globales</b>: no pertenecen a un usuario. El estado
 * leído/no leído es compartido por todo el equipo, así que si alguien marca una
 * alerta como leída deja de estar pendiente para todos. Por eso no hay tabla
 * intermedia usuario-notificación.</p>
 *
 * <p>El nombre y el sku del producto se guardan como snapshot del momento de la
 * alerta: la notificación describe un hecho pasado y no debe cambiar si luego
 * renombran el producto.</p>
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_name", nullable = false, length = 150)
    private String productName;

    @Column(name = "product_sku", nullable = false, length = 50)
    private String productSku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}

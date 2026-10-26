package edu.campus.auction.data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auction_orders", uniqueConstraints = @UniqueConstraint(columnNames = "auction_id"))
public class AuctionOrder {
    public enum Status { PENDING_PAYMENT, PAID }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "auction_id", nullable = false)
    public Long auctionId;
    @Column(nullable = false)
    public Long sellerId;
    @Column(nullable = false)
    public Long buyerId;
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public Status status;
    @Column(nullable = false)
    public Instant createdAt;
    public Instant paidAt;
}

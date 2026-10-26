package edu.campus.auction.data;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "bids", indexes = @Index(columnList = "auction_id,id"))
public class Bid {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "auction_id", nullable = false)
    public Long auctionId;
    @Column(nullable = false)
    public Long bidderId;
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal amount;
    @Column(nullable = false)
    public Instant createdAt;
}

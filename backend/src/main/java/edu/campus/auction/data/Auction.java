package edu.campus.auction.data;

import edu.campus.auction.domain.AuctionSnapshot;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "auctions")
public class Auction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false)
    public Long sellerId;
    @Column(nullable = false, length = 100)
    public String title;
    @Column(nullable = false, length = 2000)
    public String description;
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal startingPrice;
    @Column(nullable = false, precision = 12, scale = 2)
    public BigDecimal minimumIncrement;
    @Column(precision = 12, scale = 2)
    public BigDecimal currentHighestPrice;
    public Long highestBidderId;
    @Column(nullable = false)
    public Instant createdAt;
    @Column(nullable = false)
    public Instant endsAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    public AuctionSnapshot.Status status;
    // Nullable for databases created by the first release; null means no image.
    public Boolean hasImage = false;

    public AuctionSnapshot snapshot() {
        return new AuctionSnapshot(sellerId, startingPrice, minimumIncrement,
                currentHighestPrice, endsAt, status);
    }
}

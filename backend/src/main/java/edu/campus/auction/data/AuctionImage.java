package edu.campus.auction.data;

import jakarta.persistence.*;

@Entity
@Table(name = "auction_images")
public class AuctionImage {
    @Id public Long auctionId;
    @Lob @Column(nullable = false) public byte[] png;
}

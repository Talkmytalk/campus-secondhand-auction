package edu.campus.auction.data;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
    Page<Bid> findByAuctionIdOrderByIdDesc(Long auctionId, Pageable pageable);
}

package edu.campus.auction.data;

import edu.campus.auction.domain.AuctionSnapshot;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a where a.id = :id")
    Optional<Auction> findForUpdate(@Param("id") Long id);

    Page<Auction> findAllByOrderByIdDesc(Pageable pageable);

    @Query("select a.id from Auction a where a.status = :status and a.endsAt <= :now order by a.id")
    List<Long> findExpiredIds(@Param("status") AuctionSnapshot.Status status,
            @Param("now") Instant now, Pageable pageable);

    @Query("select a.id from Auction a where a.status = :status and a.endsAt <= :now and (a.sellerId = :userId or a.highestBidderId = :userId) order by a.id")
    List<Long> findExpiredIdsForUser(@Param("status") AuctionSnapshot.Status status,
            @Param("now") Instant now, @Param("userId") Long userId);
}

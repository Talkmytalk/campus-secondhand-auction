package edu.campus.auction.data;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<AuctionOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from AuctionOrder o where o.id = :id")
    Optional<AuctionOrder> findForUpdate(@Param("id") Long id);

    @Query("select o from AuctionOrder o where o.buyerId = :userId or o.sellerId = :userId order by o.id desc")
    Page<AuctionOrder> findMine(@Param("userId") Long userId, Pageable pageable);
}

package edu.campus.auction.service;

import edu.campus.auction.data.*;
import edu.campus.auction.domain.AuctionSnapshot;
import edu.campus.auction.web.ApiModels.AuctionView;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {
    private final AuctionRepository auctions;
    private final OrderRepository orders;
    private final Clock clock;

    public SettlementService(AuctionRepository auctions, OrderRepository orders, Clock clock) {
        this.auctions = auctions;
        this.orders = orders;
        this.clock = clock;
    }

    /** 与出价使用同一把数据库行锁，截止判断在获得锁后执行。 */
    @Transactional
    public AuctionView settle(Long id) {
        Auction a = auctions.findForUpdate(id).orElseThrow(
                () -> new ApiException(404, "AUCTION_NOT_FOUND", "拍卖不存在"));
        Instant now = clock.instant();
        if (a.status == AuctionSnapshot.Status.RUNNING && !now.isBefore(a.endsAt)) {
            if (a.highestBidderId == null) {
                a.status = AuctionSnapshot.Status.UNSOLD;
            } else {
                a.status = AuctionSnapshot.Status.SOLD;
                AuctionOrder order = new AuctionOrder();
                order.auctionId = a.id;
                order.sellerId = a.sellerId;
                order.buyerId = a.highestBidderId;
                order.amount = a.currentHighestPrice;
                order.status = AuctionOrder.Status.PENDING_PAYMENT;
                order.createdAt = now;
                orders.save(order);
            }
        }
        return AuctionView.of(a);
    }
}

package edu.campus.auction.service;

import edu.campus.auction.data.*;
import edu.campus.auction.domain.*;
import edu.campus.auction.web.ApiModels.*;
import java.time.Clock;
import java.time.Instant;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionService {
    private final AuctionRepository auctions;
    private final BidRepository bids;
    private final SettlementService settlement;
    private final Clock clock;

    public AuctionService(AuctionRepository auctions, BidRepository bids,
            SettlementService settlement, Clock clock) {
        this.auctions = auctions;
        this.bids = bids;
        this.settlement = settlement;
        this.clock = clock;
    }

    @Transactional
    public AuctionView create(Long sellerId, CreateAuction input) {
        Instant now = clock.instant();
        if (!input.endsAt().isAfter(now)) {
            throw new ApiException(400, "INVALID_DEADLINE", "截止时间必须晚于服务端当前时间");
        }
        Auction a = new Auction();
        a.sellerId = sellerId;
        a.title = input.title().strip();
        a.description = input.description().strip();
        a.startingPrice = Money.positive(input.startingPrice());
        a.minimumIncrement = Money.positive(input.minimumIncrement());
        a.createdAt = now;
        a.endsAt = input.endsAt();
        a.status = AuctionSnapshot.Status.RUNNING;
        return AuctionView.of(auctions.saveAndFlush(a));
    }

    public PageView<AuctionView> list(int page, int size) {
        var rows = auctions.findAllByOrderByIdDesc(PageRequest.of(page, size));
        return PageView.of(rows.map(a -> settlement.settle(a.id)));
    }

    public AuctionView get(Long id) { return settlement.settle(id); }

    @Transactional
    public BidView placeBid(Long auctionId, Long bidderId, PlaceBid input) {
        Auction a = auctions.findForUpdate(auctionId).orElseThrow(
                () -> new ApiException(404, "AUCTION_NOT_FOUND", "拍卖不存在"));
        // 先获得锁，再读取时间；排队等待锁的请求可能已经过了截止时刻。
        Instant now = clock.instant();
        var amount = BidRules.validate(a.snapshot(), bidderId, input.amount(), now);
        Bid bid = new Bid();
        bid.auctionId = a.id;
        bid.bidderId = bidderId;
        bid.amount = amount;
        bid.createdAt = now;
        bids.saveAndFlush(bid);
        a.currentHighestPrice = amount;
        a.highestBidderId = bidderId;
        return BidView.of(bid);
    }

    public PageView<BidView> history(Long auctionId, int page, int size) {
        settlement.settle(auctionId);
        return PageView.of(bids.findByAuctionIdOrderByIdDesc(auctionId,
                PageRequest.of(page, size)).map(BidView::of));
    }
}

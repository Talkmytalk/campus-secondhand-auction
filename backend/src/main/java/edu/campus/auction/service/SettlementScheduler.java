package edu.campus.auction.service;

import edu.campus.auction.data.AuctionRepository;
import edu.campus.auction.domain.AuctionSnapshot;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SettlementScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(SettlementScheduler.class);
    private final AuctionRepository auctions;
    private final SettlementService settlement;
    private final Clock clock;

    public SettlementScheduler(AuctionRepository auctions, SettlementService settlement, Clock clock) {
        this.auctions = auctions;
        this.settlement = settlement;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${auction.settlement-delay-ms}")
    public void settleExpired() {
        for (Long id : auctions.findExpiredIds(AuctionSnapshot.Status.RUNNING,
                clock.instant(), PageRequest.of(0, 100))) {
            try { settlement.settle(id); }
            catch (RuntimeException e) { LOG.error("拍卖 {} 结算失败，将在后续轮次重试", id, e); }
        }
    }
}

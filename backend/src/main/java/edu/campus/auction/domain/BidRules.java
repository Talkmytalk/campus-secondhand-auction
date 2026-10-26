package edu.campus.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * 单次出价的规则校验。未来接入数据库时，必须在锁定并读取最新拍卖记录后调用，
 * 再在同一事务中保存出价；仅调用本校验器不构成并发控制。
 */
public final class BidRules {
    private BidRules() {}

    public static BigDecimal validate(AuctionSnapshot auction, long bidderId,
            BigDecimal offeredPrice, Instant serverNow) {
        Objects.requireNonNull(auction, "拍卖信息不能为空");
        Objects.requireNonNull(serverNow, "服务端时间不能为空");
        if (bidderId <= 0) {
            throw new RuleViolation("LOGIN_REQUIRED", "请先登录");
        }
        if (auction.sellerId() == bidderId) {
            throw new RuleViolation("SELF_BID", "不能对自己发布的物品出价");
        }
        if (auction.status() != AuctionSnapshot.Status.RUNNING
                || !serverNow.isBefore(auction.endsAt())) {
            throw new RuleViolation("AUCTION_ENDED", "拍卖已结束，不能继续出价");
        }
        BigDecimal normalizedPrice = Money.positive(offeredPrice);
        BigDecimal minimum = auction.nextMinimumPrice();
        if (normalizedPrice.compareTo(minimum) < 0) {
            throw new RuleViolation("BID_TOO_LOW", "本次出价至少为 " + minimum.toPlainString() + " 元");
        }
        return normalizedPrice;
    }
}

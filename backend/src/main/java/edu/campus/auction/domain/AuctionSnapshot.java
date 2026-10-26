package edu.campus.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** currentHighestPrice 为 null 表示还没有有效出价，不能用起拍价冒充已有出价。 */
public record AuctionSnapshot(long sellerId, BigDecimal startingPrice,
        BigDecimal minimumIncrement, BigDecimal currentHighestPrice,
        Instant endsAt, Status status) {

    public enum Status { RUNNING, SOLD, UNSOLD }

    public AuctionSnapshot {
        if (sellerId <= 0) {
            throw new IllegalArgumentException("卖家编号必须大于零");
        }
        startingPrice = Money.positive(startingPrice);
        minimumIncrement = Money.positive(minimumIncrement);
        if (currentHighestPrice != null) {
            currentHighestPrice = Money.positive(currentHighestPrice);
            if (currentHighestPrice.compareTo(startingPrice) < 0) {
                throw new IllegalArgumentException("已有最高价不能低于起拍价");
            }
        }
        Objects.requireNonNull(endsAt, "截止时间不能为空");
        Objects.requireNonNull(status, "拍卖状态不能为空");
    }

    public BigDecimal nextMinimumPrice() {
        return currentHighestPrice == null
                ? startingPrice : currentHighestPrice.add(minimumIncrement);
    }
}

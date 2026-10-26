package edu.campus.auction.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class BidRulesTest {
    private static final Instant ENDS_AT = Instant.parse("2026-10-25T12:00:00Z");
    private static final Instant BEFORE_END = ENDS_AT.minusSeconds(1);

    private AuctionSnapshot auction(String currentHighestPrice) {
        return new AuctionSnapshot(1, new BigDecimal("20"), new BigDecimal("2"),
                currentHighestPrice == null ? null : new BigDecimal(currentHighestPrice),
                ENDS_AT, AuctionSnapshot.Status.RUNNING);
    }

    private void rejected(String code, AuctionSnapshot snapshot, long bidder,
            BigDecimal amount, Instant now) {
        RuleViolation error = assertThrows(RuleViolation.class,
                () -> BidRules.validate(snapshot, bidder, amount, now));
        assertEquals(code, error.code());
    }

    @Test
    void firstBidMayEqualStartingPrice() {
        assertEquals(new BigDecimal("20.00"),
                BidRules.validate(auction(null), 2, new BigDecimal("20"), BEFORE_END));
    }

    @Test
    void firstBidBelowStartingPriceIsRejected() {
        rejected("BID_TOO_LOW", auction(null), 2, new BigDecimal("19.99"), BEFORE_END);
    }

    @Test
    void subsequentBidMayEqualCurrentPricePlusIncrement() {
        assertEquals(new BigDecimal("22.00"),
                BidRules.validate(auction("20"), 2, new BigDecimal("22"), BEFORE_END));
    }

    @Test
    void subsequentBidDoesNotNeedToBeAMultipleOfIncrement() {
        assertEquals(new BigDecimal("23.00"),
                BidRules.validate(auction("20"), 2, new BigDecimal("23"), BEFORE_END));
    }

    @Test
    void higherButInsufficientIncrementIsRejected() {
        rejected("BID_TOO_LOW", auction("20"), 2, new BigDecimal("21.99"), BEFORE_END);
    }

    @Test
    void equalPriceIsRejected() {
        rejected("BID_TOO_LOW", auction("20"), 2, new BigDecimal("20"), BEFORE_END);
    }

    @Test
    void sellerCannotBid() {
        rejected("SELF_BID", auction(null), 1, new BigDecimal("30"), BEFORE_END);
    }

    @Test
    void missingIdentityIsRejected() {
        rejected("LOGIN_REQUIRED", auction(null), 0, new BigDecimal("30"), BEFORE_END);
    }

    static Stream<Arguments> invalidAmounts() {
        return Stream.of(Arguments.of((Object) null), Arguments.of(BigDecimal.ZERO),
                Arguments.of(new BigDecimal("-1")), Arguments.of(new BigDecimal("30.001")));
    }

    @ParameterizedTest
    @MethodSource("invalidAmounts")
    void invalidAmountsAreRejected(BigDecimal amount) {
        rejected("INVALID_AMOUNT", auction(null), 2, amount, BEFORE_END);
    }

    @Test
    void bidAtExactDeadlineIsRejectedEvenIfStoredStatusIsStillRunning() {
        rejected("AUCTION_ENDED", auction(null), 2, new BigDecimal("30"), ENDS_AT);
    }

    @Test
    void bidAfterDeadlineIsRejected() {
        rejected("AUCTION_ENDED", auction(null), 2, new BigDecimal("30"), ENDS_AT.plusSeconds(1));
    }

    @Test
    void storedEndedStatusIsRejected() {
        AuctionSnapshot ended = new AuctionSnapshot(1, new BigDecimal("20"),
                new BigDecimal("2"), new BigDecimal("30"), ENDS_AT, AuctionSnapshot.Status.SOLD);
        rejected("AUCTION_ENDED", ended, 2, new BigDecimal("40"), BEFORE_END);
    }
}

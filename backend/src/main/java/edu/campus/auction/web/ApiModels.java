package edu.campus.auction.web;

import edu.campus.auction.data.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;

public final class ApiModels {
    private ApiModels() {}
    public record Credentials(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,32}", message = "用户名需为3—32位字母、数字或下划线") String username,
            @NotNull @Size(min = 8, max = 64) @Pattern(regexp = "[\\x21-\\x7E]+", message = "密码需为不含空格的英文字符、数字或符号") String password) {}

    public record CreateAuction(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal startingPrice,
            @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal minimumIncrement,
            @NotNull Instant endsAt) {}

    public record PlaceBid(
            @NotNull @DecimalMin("0.01") @DecimalMax("9999999999.99") @Digits(integer = 10, fraction = 2) BigDecimal amount) {}

    public record UserView(Long id, String username) {
        public static UserView of(UserAccount user) { return new UserView(user.id, user.username); }
    }
    public record LoginView(String token, String tokenType, Instant expiresAt, UserView user) {}
    public record AuctionView(Long id, Long sellerId, String title, String description,
            BigDecimal startingPrice, BigDecimal minimumIncrement, BigDecimal currentHighestPrice,
            BigDecimal nextMinimumPrice, Long highestBidderId, String status, Instant createdAt, Instant endsAt) {
        public static AuctionView of(Auction a) {
            return new AuctionView(a.id, a.sellerId, a.title, a.description, a.startingPrice,
                    a.minimumIncrement, a.currentHighestPrice,
                    a.status == edu.campus.auction.domain.AuctionSnapshot.Status.RUNNING ? a.snapshot().nextMinimumPrice() : null,
                    a.highestBidderId, a.status.name(), a.createdAt, a.endsAt);
        }
    }
    public record BidView(Long id, Long auctionId, Long bidderId, BigDecimal amount, Instant createdAt) {
        public static BidView of(Bid b) { return new BidView(b.id, b.auctionId, b.bidderId, b.amount, b.createdAt); }
    }
    public record OrderView(Long id, Long auctionId, Long sellerId, Long buyerId, BigDecimal amount,
            String status, Instant createdAt, Instant paidAt, boolean simulated) {
        public static OrderView of(AuctionOrder o) {
            return new OrderView(o.id, o.auctionId, o.sellerId, o.buyerId, o.amount,
                    o.status.name(), o.createdAt, o.paidAt, true);
        }
    }
    public record PageView<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
        public static <T> PageView<T> of(Page<T> page) {
            return new PageView<>(page.getContent(), page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages());
        }
    }
}

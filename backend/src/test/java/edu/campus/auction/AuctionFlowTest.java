package edu.campus.auction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.campus.auction.data.*;
import edu.campus.auction.service.SettlementScheduler;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flow-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "auction.settlement-delay-ms=3600000"})
@AutoConfigureMockMvc
@Import(AuctionFlowTest.TestClockConfiguration.class)
class AuctionFlowTest {
    static class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>();
        void set(Instant value) { now.set(value); }
        void advance(long seconds) { now.updateAndGet(t -> t.plusSeconds(seconds)); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
    @TestConfiguration
    static class TestClockConfiguration {
        @Bean @Primary
        MutableClock mutableClock() {
            MutableClock clock = new MutableClock();
            clock.set(Instant.parse("2026-10-19T10:00:00Z"));
            return clock;
        }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;
    @Autowired UserRepository users;
    @Autowired SessionRepository sessions;
    @Autowired AuctionRepository auctions;
    @Autowired BidRepository bids;
    @Autowired OrderRepository orders;
    @Autowired SettlementScheduler scheduler;
    Account seller, buyer, other;
    record Account(long id, String token) {}

    @BeforeEach
    void prepare() throws Exception {
        orders.deleteAll(); bids.deleteAll(); auctions.deleteAll(); sessions.deleteAll(); users.deleteAll();
        clock.set(Instant.parse("2026-10-19T10:00:00Z"));
        seller = account("seller"); buyer = account("buyer"); other = account("other");
    }

    JsonNode call(String method, String path, String token, Object body, int expectedStatus) throws Exception {
        MvcResult result = send(method, path, token, body);
        String content = result.getResponse().getContentAsString();
        assertEquals(expectedStatus, result.getResponse().getStatus(), content);
        return content.isEmpty() ? json.createObjectNode() : json.readTree(content);
    }

    MvcResult send(String method, String path, String token, Object body) throws Exception {
        MockHttpServletRequestBuilder request = request(HttpMethod.valueOf(method), path);
        if (token != null) { request.header("Authorization", "Bearer " + token); }
        if (body != null) { request.contentType("application/json").content(json.writeValueAsString(body)); }
        return mvc.perform(request).andReturn();
    }

    Account account(String name) throws Exception {
        var credentials = Map.of("username", name, "password", "Password123");
        JsonNode user = call("POST", "/api/auth/register", null, credentials, 201);
        JsonNode login = call("POST", "/api/auth/login", null, credentials, 200);
        return new Account(user.get("id").asLong(), login.get("token").asText());
    }

    Map<String, Object> item() {
        return new HashMap<>(Map.of("title", "二手台灯", "description", "功能正常，校园内交付",
                "startingPrice", "20.00", "minimumIncrement", "2.00",
                "endsAt", clock.instant().plusSeconds(60).toString()));
    }

    long create() throws Exception {
        return call("POST", "/api/auctions", seller.token, item(), 201).get("id").asLong();
    }

    JsonNode bid(long id, Account account, String amount, int status) throws Exception {
        return call("POST", "/api/auctions/" + id + "/bids", account.token,
                Map.of("amount", amount), status);
    }

    long payableOrder() throws Exception {
        long id = create(); bid(id, buyer, "20", 201); clock.advance(60);
        return call("GET", "/api/orders/mine", buyer.token, null, 200)
                .get("items").get(0).get("id").asLong();
    }

    @Test
    void completeAuctionSelectsWinnerAndOnlyWinnerCanPayIdempotently() throws Exception {
        long id = create();
        bid(id, buyer, "20", 201); bid(id, other, "22", 201);
        clock.advance(60);
        JsonNode ended = call("GET", "/api/auctions/" + id, null, null, 200);
        assertEquals("SOLD", ended.get("status").asText());
        assertEquals(other.id, ended.get("highestBidderId").asLong());
        assertTrue(ended.get("nextMinimumPrice").isNull());
        JsonNode mine = call("GET", "/api/orders/mine", other.token, null, 200);
        long order = mine.get("items").get(0).get("id").asLong();
        assertEquals(new BigDecimal("22.00"), mine.get("items").get(0).get("amount").decimalValue().setScale(2));
        call("POST", "/api/orders/" + order + "/simulate-payment", buyer.token, null, 403);
        call("POST", "/api/orders/" + order + "/simulate-payment", seller.token, null, 403);
        // 使用纳秒时间，确认首次响应与数据库保存后的重复付款响应精度一致。
        clock.set(clock.instant().plusNanos(123_456_789));
        JsonNode paid = call("POST", "/api/orders/" + order + "/simulate-payment", other.token, null, 200);
        assertEquals("PAID", paid.get("status").asText());
        assertTrue(paid.get("simulated").asBoolean());
        clock.advance(5);
        JsonNode repeated = call("POST", "/api/orders/" + order + "/simulate-payment", other.token, null, 200);
        assertEquals(paid.get("paidAt"), repeated.get("paidAt"));
        assertEquals(1, orders.count());
        assertEquals(0, call("GET", "/api/orders/mine", buyer.token, null, 200).get("totalElements").asInt());
        assertEquals(1, call("GET", "/api/orders/mine", seller.token, null, 200).get("totalElements").asInt());
    }

    @Test
    void rejectedBidsDoNotChangePriceOrHistory() throws Exception {
        long id = create();
        assertEquals("SELF_BID", bid(id, seller, "20", 400).get("code").asText());
        bid(id, buyer, "19.99", 400);
        bid(id, buyer, "20", 201);
        bid(id, other, "21.99", 400);
        bid(id, other, "20", 400);
        JsonNode detail = call("GET", "/api/auctions/" + id, null, null, 200);
        assertEquals(buyer.id, detail.get("highestBidderId").asLong());
        assertEquals(1, call("GET", "/api/auctions/" + id + "/bids", null, null, 200).get("totalElements").asInt());
    }

    @Test
    void identityComesFromTokenInsteadOfRequestBody() throws Exception {
        var input = item(); input.put("sellerId", buyer.id);
        JsonNode created = call("POST", "/api/auctions", seller.token, input, 201);
        assertEquals(seller.id, created.get("sellerId").asLong());
        JsonNode accepted = call("POST", "/api/auctions/" + created.get("id").asLong() + "/bids",
                buyer.token, Map.of("amount", "20", "bidderId", other.id), 201);
        assertEquals(buyer.id, accepted.get("bidderId").asLong());
        call("POST", "/api/auctions", null, item(), 401);
        call("POST", "/api/auctions/" + created.get("id").asLong() + "/bids", null, Map.of("amount", "22"), 401);
    }

    @Test
    void exactDeadlineBlocksBidBeforeSchedulerHasSettledAuction() throws Exception {
        long id = create(); clock.advance(60);
        assertEquals("AUCTION_ENDED", bid(id, buyer, "20", 400).get("code").asText());
        assertEquals(0, bids.count());
    }

    @Test
    void noBidAuctionBecomesUnsoldAndDoesNotCreateOrder() throws Exception {
        long id = create(); clock.advance(60);
        assertEquals("UNSOLD", call("GET", "/api/auctions/" + id, null, null, 200).get("status").asText());
        assertEquals(0, orders.count());
    }

    @Test
    void backgroundSettlementIsRepeatableWithoutDuplicateOrders() throws Exception {
        long id = create(); bid(id, buyer, "20", 201); clock.advance(60);
        scheduler.settleExpired(); scheduler.settleExpired();
        assertEquals(1, orders.count());
        assertEquals("SOLD", auctions.findById(id).orElseThrow().status.name());
    }

    @Test
    void invalidFieldsAndPaginationReturnBadRequest() throws Exception {
        long id = create();
        for (String amount : List.of("0", "-1", "30.001", "10000000000.00", "abc")) {
            bid(id, buyer, amount, 400);
        }
        var input = item(); input.put("endsAt", clock.instant().toString());
        call("POST", "/api/auctions", seller.token, input, 400);
        input = item(); input.put("title", "   ");
        call("POST", "/api/auctions", seller.token, input, 400);
        call("GET", "/api/auctions?page=-1", null, null, 400);
        call("GET", "/api/auctions?size=101", null, null, 400);
        call("GET", "/api/auctions/not-a-number", null, null, 400);
        call("GET", "/api/auctions/999999", null, null, 404);
    }

    @Test
    void registrationValidatesCredentialsAndDoesNotExposePasswordHash() throws Exception {
        call("POST", "/api/auth/register", null, Map.of("username", "seller", "password", "Password123"), 409);
        call("POST", "/api/auth/register", null, Map.of("username", "x", "password", "Password123"), 400);
        call("POST", "/api/auth/register", null, Map.of("username", "valid_user", "password", "short"), 400);
        call("POST", "/api/auth/register", null, Map.of("username", "valid_user", "password", "x".repeat(65)), 400);
        JsonNode me = call("GET", "/api/auth/me", seller.token, null, 200);
        assertFalse(me.has("passwordHash")); assertFalse(me.has("password"));
        String stored = users.findByUsername("seller").orElseThrow().passwordHash;
        assertNotEquals("Password123", stored); assertTrue(stored.startsWith("$2"));
        assertFalse(sessions.findAll().stream().anyMatch(s -> s.tokenHash.equals(seller.token)));
    }

    @Test
    void loginLogoutAndTokenExpiryAreEnforced() throws Exception {
        call("POST", "/api/auth/login", null, Map.of("username", "seller", "password", "WrongPassword"), 401);
        call("POST", "/api/auth/logout", seller.token, null, 204);
        call("GET", "/api/auth/me", seller.token, null, 401);
        call("GET", "/api/auth/me", "forged", null, 401);
        clock.advance(24 * 60 * 60);
        call("GET", "/api/auth/me", buyer.token, null, 401);
    }

    @Test
    void unrelatedUsersCannotReadOrdersAndPaymentAmountCannotBeChanged() throws Exception {
        long order = payableOrder();
        call("GET", "/api/orders/" + order, other.token, null, 403);
        call("POST", "/api/orders/" + order + "/simulate-payment", other.token, null, 403);
        JsonNode paid = call("POST", "/api/orders/" + order + "/simulate-payment", buyer.token,
                Map.of("amount", "0.01", "buyerId", other.id), 200);
        assertEquals(0, new BigDecimal("20").compareTo(paid.get("amount").decimalValue()));
        assertEquals(buyer.id, paid.get("buyerId").asLong());
    }

    @Test
    void simultaneousEqualBidsAcceptOnlyOneRequest() throws Exception {
        long id = create();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (Account participant : List.of(buyer, other)) {
                results.add(pool.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return send("POST", "/api/auctions/" + id + "/bids", participant.token,
                            Map.of("amount", "20")).getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> result : results) { statuses.add(result.get(15, TimeUnit.SECONDS)); }
            Collections.sort(statuses);
            assertEquals(List.of(201, 400), statuses);
            assertEquals(1, bids.count());
        } finally { pool.shutdownNow(); }
    }

    @Test
    void simultaneousPaymentClicksPreserveOneOrder() throws Exception {
        long order = payableOrder();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                results.add(pool.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return send("POST", "/api/orders/" + order + "/simulate-payment", buyer.token,
                            null).getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> result : results) { assertEquals(200, result.get(15, TimeUnit.SECONDS)); }
            assertEquals(1, orders.count());
            assertEquals(AuctionOrder.Status.PAID, orders.findById(order).orElseThrow().status);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void localFrontendCorsPreflightIsAllowed() throws Exception {
        MvcResult response = mvc.perform(options("/api/auctions")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")).andReturn();
        assertEquals(200, response.getResponse().getStatus());
        assertEquals("http://localhost:5173", response.getResponse().getHeader("Access-Control-Allow-Origin"));
    }
}

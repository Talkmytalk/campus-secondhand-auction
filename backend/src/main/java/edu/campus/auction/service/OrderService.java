package edu.campus.auction.service;

import edu.campus.auction.data.*;
import edu.campus.auction.domain.AuctionSnapshot;
import edu.campus.auction.web.ApiModels.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final AuctionRepository auctions;
    private final SettlementService settlement;
    private final Clock clock;

    public OrderService(OrderRepository orders, AuctionRepository auctions,
            SettlementService settlement, Clock clock) {
        this.orders = orders;
        this.auctions = auctions;
        this.settlement = settlement;
        this.clock = clock;
    }

    public PageView<OrderView> mine(Long userId, int page, int size) {
        // 即使后台结算任务还没运行到这一场，查询时也会先生成应有的订单。
        for (Long id : auctions.findExpiredIdsForUser(AuctionSnapshot.Status.RUNNING,
                clock.instant(), userId)) {
            settlement.settle(id);
        }
        return PageView.of(orders.findMine(userId, PageRequest.of(page, size)).map(OrderView::of));
    }

    public OrderView get(Long userId, Long orderId) {
        AuctionOrder order = orders.findById(orderId).orElseThrow(OrderService::notFound);
        if (!order.buyerId.equals(userId) && !order.sellerId.equals(userId)) {
            throw new ApiException(403, "ORDER_FORBIDDEN", "只能查看与自己有关的订单");
        }
        return OrderView.of(order);
    }

    @Transactional
    public OrderView simulatePayment(Long userId, Long orderId) {
        AuctionOrder order = orders.findForUpdate(orderId).orElseThrow(OrderService::notFound);
        if (!order.buyerId.equals(userId)) {
            throw new ApiException(403, "BUYER_ONLY", "只有获胜买家可以进行模拟付款");
        }
        if (order.status == AuctionOrder.Status.PENDING_PAYMENT) {
            order.status = AuctionOrder.Status.PAID;
            // 与数据库的微秒精度一致，使首次付款和重复请求返回同一时间。
            order.paidAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        }
        // 已付款时直接返回已有结果，实现重复点击不重复处理。
        return OrderView.of(order);
    }

    private static ApiException notFound() {
        return new ApiException(404, "ORDER_NOT_FOUND", "订单不存在");
    }
}

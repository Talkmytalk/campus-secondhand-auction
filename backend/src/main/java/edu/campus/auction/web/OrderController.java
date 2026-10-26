package edu.campus.auction.web;

import edu.campus.auction.service.*;
import edu.campus.auction.web.ApiModels.*;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated
@RequestMapping("/api/orders")
public class OrderController {
    private final AuthService auth;
    private final OrderService orders;
    public OrderController(AuthService auth, OrderService orders) { this.auth = auth; this.orders = orders; }

    @GetMapping("/mine")
    public PageView<OrderView> mine(@RequestHeader(value = "Authorization", required = false) String header,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return orders.mine(auth.requireUser(header), page, size);
    }

    @GetMapping("/{id}")
    public OrderView get(@PathVariable @Min(1) Long id,
            @RequestHeader(value = "Authorization", required = false) String header) {
        return orders.get(auth.requireUser(header), id);
    }

    @PostMapping("/{id}/simulate-payment")
    public OrderView pay(@PathVariable @Min(1) Long id,
            @RequestHeader(value = "Authorization", required = false) String header) {
        return orders.simulatePayment(auth.requireUser(header), id);
    }
}

package edu.campus.auction.web;

import edu.campus.auction.service.*;
import edu.campus.auction.web.ApiModels.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated
@RequestMapping("/api/auctions")
public class AuctionController {
    private final AuthService auth;
    private final AuctionService auctions;
    public AuctionController(AuthService auth, AuctionService auctions) {
        this.auth = auth; this.auctions = auctions;
    }

    @GetMapping
    public PageView<AuctionView> list(@RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return auctions.list(page, size);
    }

    @GetMapping("/{id}")
    public AuctionView get(@PathVariable @Min(1) Long id) { return auctions.get(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public AuctionView create(@RequestHeader(value = "Authorization", required = false) String header,
            @Valid @RequestBody CreateAuction input) {
        return auctions.create(auth.requireUser(header), input);
    }

    @PostMapping("/{id}/bids") @ResponseStatus(HttpStatus.CREATED)
    public BidView bid(@PathVariable @Min(1) Long id,
            @RequestHeader(value = "Authorization", required = false) String header,
            @Valid @RequestBody PlaceBid input) {
        return auctions.placeBid(id, auth.requireUser(header), input);
    }

    @GetMapping("/{id}/bids")
    public PageView<BidView> history(@PathVariable @Min(1) Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return auctions.history(id, page, size);
    }
}

package edu.campus.auction.web;

import edu.campus.auction.service.*;
import edu.campus.auction.web.ApiModels.AuctionView;
import jakarta.validation.constraints.Min;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController @Validated
@RequestMapping("/api/auctions/{id}/image")
public class AuctionImageController {
    private final AuthService auth;
    private final AuctionImageService images;
    public AuctionImageController(AuthService auth, AuctionImageService images) {
        this.auth = auth; this.images = images;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuctionView upload(@PathVariable @Min(1) Long id,
            @RequestHeader(value = "Authorization", required = false) String header,
            @RequestParam("file") MultipartFile file) {
        return images.upload(id, auth.requireUser(header), file);
    }

    @GetMapping
    public ResponseEntity<byte[]> read(@PathVariable @Min(1) Long id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline; filename=auction.png").body(images.read(id));
    }
}

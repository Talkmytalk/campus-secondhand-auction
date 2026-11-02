package edu.campus.auction.service;

import edu.campus.auction.data.*;
import edu.campus.auction.domain.AuctionSnapshot.Status;
import edu.campus.auction.web.ApiModels.AuctionView;
import java.io.*;
import java.time.Clock;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AuctionImageService {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final AuctionRepository auctions;
    private final AuctionImageRepository images;
    private final Clock clock;

    public AuctionImageService(AuctionRepository auctions, AuctionImageRepository images, Clock clock) {
        this.auctions = auctions; this.images = images; this.clock = clock;
    }

    @Transactional
    public AuctionView upload(Long id, Long userId, MultipartFile file) {
        Auction a = auctions.findForUpdate(id).orElseThrow(() -> new ApiException(404, "AUCTION_NOT_FOUND", "拍卖不存在"));
        if (!a.sellerId.equals(userId)) throw new ApiException(403, "FORBIDDEN", "只有发布者可以上传图片");
        if (a.status != Status.RUNNING || !clock.instant().isBefore(a.endsAt) || a.highestBidderId != null)
            throw new ApiException(409, "IMAGE_LOCKED", "已有出价或拍卖已结束，不能修改图片");
        byte[] png = normalize(file);
        AuctionImage image = new AuctionImage(); image.auctionId = id; image.png = png;
        images.save(image); a.hasImage = true;
        return AuctionView.of(a);
    }

    @Transactional(readOnly = true)
    public byte[] read(Long id) {
        return images.findById(id).orElseThrow(() -> new ApiException(404, "IMAGE_NOT_FOUND", "商品图片不存在")).png;
    }

    private byte[] normalize(MultipartFile file) {
        if (file.isEmpty()) throw invalid();
        if (file.getSize() > MAX_BYTES) throw new ApiException(413, "IMAGE_TOO_LARGE", "图片不能超过5MB");
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).matches(".*\\.(png|jpg|jpeg)$")) throw invalid();
        try (var input = new MemoryCacheImageInputStream(file.getInputStream())) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw invalid();
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName();
                if (!format.equalsIgnoreCase("PNG") && !format.equalsIgnoreCase("JPEG")) throw invalid();
                int w = reader.getWidth(0), h = reader.getHeight(0);
                if (w < 1 || h < 1 || w > 4096 || h > 4096 || (long) w * h > 16_000_000)
                    throw new ApiException(400, "IMAGE_DIMENSIONS", "图片边长不能超过4096像素，总像素不能超过1600万");
                var decoded = reader.read(0);
                var output = new ByteArrayOutputStream();
                // Re-encode pixels: do not serve user filenames, metadata or appended payloads.
                if (decoded == null || !ImageIO.write(decoded, "PNG", output)) throw invalid();
                if (output.size() > MAX_BYTES) throw new ApiException(413, "IMAGE_TOO_LARGE", "转换后的图片超过5MB，请压缩后重试");
                return output.toByteArray();
            } finally { reader.dispose(); }
        } catch (IOException | IllegalArgumentException e) { throw invalid(); }
    }

    private ApiException invalid() { return new ApiException(400, "IMAGE_INVALID", "请上传有效且完整的JPG或PNG图片"); }
}

package com.companion.config;

import com.companion.image.ImageDownloadTransport;
import com.companion.image.ImageGenerationClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

/** Test-only image boundary. It is active only with the acceptance profile. */
@Configuration
@Profile("acceptance")
@ConditionalOnProperty(name = "acceptance.image.fake.enabled", havingValue = "true", matchIfMissing = true)
public class AcceptanceImageFakeConfig {
    private static byte[] fakePng(String seed) {
        BufferedImage image = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int hue = Math.floorMod(seed == null ? 0 : seed.hashCode(), 360);
        g.setColor(Color.getHSBColor(hue / 360f, .72f, .92f));
        g.fillRect(0, 0, 256, 256);
        g.setColor(new Color(255, 220, 40));
        g.fillOval(58, 28, 140, 140);
        g.setColor(new Color(181, 64, 196));
        g.fillRect(0, 178, 256, 78);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        g.drawString("测试图片", 77, 225);
        g.dispose();
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("cannot create acceptance fake image", e);
        }
    }

    @Bean
    @Primary
    ImageGenerationClient acceptanceImageGenerationClient() {
        return prompt -> {
            if (prompt != null && prompt.contains("FAIL")) throw new IOException("fake image generation failure");
            return "https://203.0.113.10/fake-image.png?seed=" + Math.abs(prompt == null ? 0 : prompt.hashCode());
        };
    }

    @Bean
    @Primary
    ImageDownloadTransport acceptanceImageDownloadTransport() {
        return uri -> {
            byte[] png = fakePng(uri.toString());
            return new ImageDownloadTransport.DownloadResponse(200, png.length, null, new ByteArrayInputStream(png));
        };
    }
}

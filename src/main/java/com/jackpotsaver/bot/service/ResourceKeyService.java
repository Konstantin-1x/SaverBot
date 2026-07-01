package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.config.SecurityProperties;
import com.jackpotsaver.bot.domain.MediaType;
import com.jackpotsaver.bot.domain.VideoQuality;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class ResourceKeyService {
    private static final String ALGORITHM = "HmacSHA256";
    private final SecretKeySpec key;

    public ResourceKeyService(SecurityProperties properties) {
        this.key = new SecretKeySpec(properties.dataHashKey().getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    public String create(String normalizedUrl, MediaType mediaType, VideoQuality quality) {
        String canonical = normalizedUrl + "|" + mediaType + "|" + quality;
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA-256 is not available", ex);
        }
    }
}


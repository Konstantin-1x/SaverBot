package com.jackpotsaver.bot.service;

import com.jackpotsaver.bot.domain.MediaType;
import com.jackpotsaver.bot.domain.Platform;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;

public class PlatformDetector {
    public Optional<VideoLink> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        URI uri = uri(raw.trim());
        if (uri == null || uri.getHost() == null || uri.getScheme() == null) {
            return Optional.empty();
        }
        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return Optional.empty();
        }
        String host = stripWww(uri.getHost().toLowerCase(Locale.ROOT));
        String path = uri.getPath() == null ? "" : uri.getPath();
        if (host.equals("youtu.be")) {
            return Optional.of(link(raw, Platform.YOUTUBE, MediaType.YOUTUBE_VIDEO));
        }
        if (host.endsWith("youtube.com") && path.startsWith("/shorts/")) {
            return Optional.of(link(raw, Platform.YOUTUBE_SHORTS, MediaType.YOUTUBE_SHORTS));
        }
        if (host.endsWith("youtube.com") && path.equals("/watch")) {
            return Optional.of(link(raw, Platform.YOUTUBE, MediaType.YOUTUBE_VIDEO));
        }
        if (host.endsWith("instagram.com") && (path.startsWith("/reel/") || path.startsWith("/p/"))) {
            return Optional.of(link(raw, Platform.INSTAGRAM, MediaType.INSTAGRAM_VIDEO));
        }
        if (host.endsWith("tiktok.com") || host.equals("vm.tiktok.com") || host.equals("vt.tiktok.com")) {
            return Optional.of(link(raw, Platform.TIKTOK, MediaType.TIKTOK_VIDEO));
        }
        return Optional.empty();
    }

    public boolean isUrl(String raw) {
        URI uri = uri(raw == null ? "" : raw.trim());
        return uri != null && uri.getScheme() != null && uri.getHost() != null;
    }

    private VideoLink link(String raw, Platform platform, MediaType mediaType) {
        return new VideoLink(raw.trim(), normalize(raw.trim()), platform, mediaType);
    }

    private String normalize(String raw) {
        URI uri = uri(raw);
        if (uri == null) {
            return raw;
        }
        String host = stripWww(uri.getHost().toLowerCase(Locale.ROOT));
        String path = trimSlash(uri.getPath());
        String query = uri.getQuery();
        if (host.equals("youtu.be") && !path.isBlank()) {
            return "https://youtu.be" + path;
        }
        if (host.equals("youtube.com") && "/watch".equals(path) && query != null) {
            String videoId = queryParam(query, "v");
            if (videoId != null) {
                return "https://youtube.com/watch?v=" + videoId;
            }
        }
        return "https://" + host + path + (query == null ? "" : "?" + query);
    }

    private URI uri(String raw) {
        try {
            return new URI(raw);
        } catch (URISyntaxException ex) {
            return null;
        }
    }

    private String stripWww(String host) {
        return host.startsWith("www.") ? host.substring(4) : host;
    }

    private String trimSlash(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        return path.endsWith("/") && path.length() > 1 ? path.substring(0, path.length() - 1) : path;
    }

    private String queryParam(String query, String name) {
        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals(name)) {
                return pair[1];
            }
        }
        return null;
    }
}

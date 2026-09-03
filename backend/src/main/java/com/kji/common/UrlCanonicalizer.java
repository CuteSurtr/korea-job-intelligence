package com.kji.common;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

public final class UrlCanonicalizer {

    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "utm_id",
            "gh_src", "lever-source", "ashby_jid_source", "source", "src", "ref", "referrer",
            "fbclid", "gclid", "msclkid", "igshid", "mc_cid", "mc_eid", "trk", "trackingid",
            "originalsubdomain", "rec_seq", "logpath", "gclsrc", "_ga", "yclid"
    );

    private static final Set<String> DEFAULT_PORTS = Set.of("80", "443");

    private UrlCanonicalizer() {
    }

    public static Optional<String> canonicalize(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            return Optional.empty();
        }
        String trimmed = rawUrl.trim();
        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException exception) {
            return Optional.empty();
        }
        if (uri.getHost() == null) {
            return Optional.empty();
        }

        String scheme = uri.getScheme() == null
                ? "https"
                : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            return Optional.empty();
        }

        String host = uri.getHost().toLowerCase(Locale.ROOT);
        if (host.startsWith("www.")) {
            host = host.substring(4);
        }

        StringBuilder builder = new StringBuilder(scheme).append("://").append(host);
        int port = uri.getPort();
        if (port > 0 && !DEFAULT_PORTS.contains(String.valueOf(port))) {
            builder.append(':').append(port);
        }

        String path = uri.getPath() == null ? "" : uri.getPath();
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        builder.append(path);

        String query = canonicalQuery(uri.getRawQuery());
        if (!query.isEmpty()) {
            builder.append('?').append(query);
        }
        return Optional.of(builder.toString());
    }

    public static Optional<String> canonicalKey(String rawUrl) {
        return canonicalize(rawUrl).map(canonical -> canonical.length() <= 600
                ? canonical
                : canonical.substring(0, 540) + "#" + Hashing.sha256(canonical).substring(0, 32));
    }

    private static String canonicalQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        TreeMap<String, List<String>> retained = new TreeMap<>();
        for (String pair : rawQuery.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            String lowerName = name.toLowerCase(Locale.ROOT);
            if (TRACKING_PARAMETERS.contains(lowerName) || lowerName.startsWith("utm_")) {
                continue;
            }
            retained.computeIfAbsent(lowerName, key -> new ArrayList<>()).add(value);
        }
        List<String> parts = new ArrayList<>();
        retained.forEach((name, values) -> {
            values.sort(String::compareTo);
            for (String value : values) {
                parts.add(value.isEmpty() ? name : name + "=" + value);
            }
        });
        return String.join("&", parts);
    }
}

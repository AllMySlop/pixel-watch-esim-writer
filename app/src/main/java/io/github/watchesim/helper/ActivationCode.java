package io.github.watchesim.helper;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The activation-code grammar recovered from Pixel Watch 5.0.0.958206140. */
public final class ActivationCode {
    private static final Pattern FORMAT = Pattern.compile(
            "^(?:LPA:)?(1\\$([0-9a-zA-Z%*+\\-./:]+)\\$([0-9A-Z\\-]*)(?:\\$([0-9.]*))?(?:\\$(1))?)$");
    public final String payload;
    public final String server;
    public final boolean needsConfirmationCode;

    private ActivationCode(String payload, String server, boolean needsConfirmationCode) {
        this.payload = payload;
        this.server = server;
        this.needsConfirmationCode = needsConfirmationCode;
    }

    public static ActivationCode parse(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException("empty");
        }
        String value = input.trim();
        if (value.length() > 259) throw new IllegalArgumentException("length");
        Matcher match = FORMAT.matcher(value);
        if (!match.matches() || match.group(1).length() > 255) {
            throw new IllegalArgumentException("format");
        }
        String server = match.group(2);
        // SM-DP+ is a server address, not an arbitrary URL, userinfo or path.
        try {
            URI uri = new URI("https://" + server);
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || !uri.getRawPath().isEmpty() || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || uri.getPort() > 65535) {
                throw new IllegalArgumentException("server");
            }
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("server");
        }
        return new ActivationCode(value, server, "1".equals(match.group(5)));
    }
}

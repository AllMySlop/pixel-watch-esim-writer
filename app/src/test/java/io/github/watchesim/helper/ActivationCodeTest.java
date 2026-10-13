package io.github.watchesim.helper;

import static org.junit.Assert.*;
import org.junit.Test;

public class ActivationCodeTest {
    @Test public void acceptsStockFormatsAndTrimsOuterWhitespace() {
        ActivationCode code = ActivationCode.parse("  LPA:1$smdp.example.com$ABC-123\n");
        assertEquals("LPA:1$smdp.example.com$ABC-123", code.payload);
        assertEquals("smdp.example.com", code.server);
        assertFalse(code.needsConfirmationCode);
        assertEquals("1$smdp.example.com$", ActivationCode.parse("1$smdp.example.com$").payload);
        assertEquals("smdp.example.com:443", ActivationCode.parse("1$smdp.example.com:443$ABC").server);
    }
    @Test public void detectsConfirmationFlag() {
        assertTrue(ActivationCode.parse("LPA:1$smdp.example.com$ABC$$1").needsConfirmationCode);
        assertTrue(ActivationCode.parse("1$smdp.example.com$ABC$1.2.3$1").needsConfirmationCode);
        assertFalse(ActivationCode.parse("1$smdp.example.com$ABC$1.2.3").needsConfirmationCode);
    }
    @Test public void rejectsMalformedUrlsControlCharactersAndUnsupportedVersion() {
        for (String code : new String[] {"", "https://example.com", "LPA:2$example.com$ABC",
                "LPA:1$example.com$abc", "LPA:1$example.com$ABC&dest=home",
                "LPA:1$example.com/path$ABC", "LPA:1$example.com:70000$ABC",
                "LPA:1$exa\nmple.com$ABC", "LPA:1$example.com$ABC\nextra"}) {
            assertThrows(code, IllegalArgumentException.class, () -> ActivationCode.parse(code));
        }
        assertThrows(IllegalArgumentException.class, () -> ActivationCode.parse(null));
    }
    @Test public void enforcesStockLengthBoundaryExcludingLpaPrefix() {
        String base = "1$example.com$";
        String exactly255 = base + "A".repeat(255 - base.length());
        assertEquals("LPA:" + exactly255, ActivationCode.parse("LPA:" + exactly255).payload);
        assertThrows(IllegalArgumentException.class, () -> ActivationCode.parse(exactly255 + "A"));
    }
}

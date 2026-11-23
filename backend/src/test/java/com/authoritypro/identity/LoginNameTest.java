package com.authoritypro.identity;

import com.authoritypro.platform.identity.domain.LoginName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoginNameTest {
    @Test
    void normalizesByStrippingAndLowercasing() {
        assertEquals("alice.owner@example.com", LoginName.of("  Alice.Owner@Example.COM ").value());
        assertEquals("abc", LoginName.of("ABC").value());
    }

    @Test
    void acceptsTheFullDatabaseAlphabet() {
        assertEquals("a._@-b", LoginName.of("A._@-B").value());
        String maximal = "a" + "b".repeat(127);
        assertEquals(128, LoginName.of(maximal).value().length());
    }

    @Test
    void rejectsInvalidOrUnboundedLogins() {
        assertThrows(IllegalArgumentException.class, () -> LoginName.of(null));
        assertThrows(IllegalArgumentException.class, () -> LoginName.of(""));
        assertThrows(IllegalArgumentException.class, () -> LoginName.of("ab"));
        assertThrows(IllegalArgumentException.class, () -> LoginName.of(".alice"));
        assertThrows(IllegalArgumentException.class, () -> LoginName.of("alice smith"));
        assertThrows(IllegalArgumentException.class, () -> LoginName.of("a" + "b".repeat(128)));
        assertThrows(IllegalArgumentException.class, () -> LoginName.of("alice/../root"));
    }
}

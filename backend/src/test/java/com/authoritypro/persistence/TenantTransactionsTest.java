package com.authoritypro.persistence;

import com.authoritypro.platform.tenancy.infrastructure.persistence.TenantTransactions;
import java.util.Arrays;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Pure argument/contract tests: never obtain a JDBC connection or start a database. */
class TenantTransactionsTest {
    private static final UUID USER = UUID.fromString("20000000-0000-0000-0000-000000000001");
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Test
    void rejectsMissingDataSource() {
        assertThrows(IllegalArgumentException.class, () -> new TenantTransactions(null));
    }

    @Test
    void rejectsMissingScopeBeforeCheckout() {
        DataSource source = mock(DataSource.class);
        TenantTransactions transactions = new TenantTransactions(source);
        assertThrows(IllegalArgumentException.class, () -> transactions.scoped(null, TENANT, false, c -> null));
        assertThrows(IllegalArgumentException.class, () -> transactions.scoped(USER, null, false, c -> null));
        assertThrows(IllegalArgumentException.class, () -> transactions.requiresNew(null, TENANT, false, c -> null));
        assertThrows(IllegalArgumentException.class, () -> transactions.requiresNew(USER, null, false, c -> null));
        assertThrows(IllegalArgumentException.class, () -> transactions.userScoped(null, true, c -> null));
        verifyNoInteractions(source);
    }

    @Test
    void rejectsMissingWorkBeforeCheckout() {
        DataSource source = mock(DataSource.class);
        TenantTransactions transactions = new TenantTransactions(source);
        assertThrows(IllegalArgumentException.class, () -> transactions.scoped(USER, TENANT, false, null));
        assertThrows(IllegalArgumentException.class, () -> transactions.requiresNew(USER, TENANT, false, null));
        assertThrows(IllegalArgumentException.class, () -> transactions.userScoped(USER, true, null));
        verifyNoInteractions(source);
    }

    @Test
    void exposesNoStringScopeParserOrRawTransactionShortcut() {
        for (var method : TenantTransactions.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
            assertFalse(Arrays.asList(method.getParameterTypes()).contains(String.class));
            assertFalse(method.getName().equals("raw"));
        }
    }
}

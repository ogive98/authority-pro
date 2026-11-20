package com.authoritypro.launch;

import com.authoritypro.platform.AuthorityProApplication;
import com.authoritypro.platform.AuthorityProApplication.SkeletonSettings;
import com.authoritypro.platform.runtime.infrastructure.persistence.LocalDatabase;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocalDatabaseInputTest {
    @Test
    void acceptsOneBoundedUtf8LineAndEofWithoutTrimming() {
        for (String terminator : new String[]{"\n", "\r\n"}) {
            for (String password : new String[]{" private-input ", "x".repeat(256), "é".repeat(128)}) {
                assertThat(LocalDatabase.readPassword(input(password + terminator))).isEqualTo(password);
            }
        }
    }

    @Test
    void refusesMissingOverlongMultilineOrMalformedSecretsWithoutRetainingTheCause() {
        for (String text : new String[]{"", "\n", "\r\n", "private-input", "x".repeat(257) + "\n",
                "é".repeat(129) + "\n", "private-input\nextra", "private-input\n\n",
                "private-input\rwrong\n", "private-input\0\n"}) {
            assertThatThrownBy(() -> LocalDatabase.readPassword(input(text)))
                    .isInstanceOf(IllegalStateException.class).hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
        }
        assertThatThrownBy(() -> LocalDatabase.readPassword(new ByteArrayInputStream(new byte[]{(byte) 0xc3, 10})))
                .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
        assertThatThrownBy(() -> LocalDatabase.readPassword(new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("private-input");
            }
        })).hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
    }

    @Test
    void overlongInputStopsAfterTheFirstExcessByteWithoutUnboundedBuffering() {
        int[] reads = {0};
        var endless = new InputStream() {
            @Override
            public int read() {
                if (++reads[0] > 257) {
                    throw new AssertionError("Reader exceeded its byte bound");
                }
                return 'x';
            }
        };
        assertThatThrownBy(() -> LocalDatabase.readPassword(endless)).hasMessage("DATABASE_STARTUP_REFUSED");
        assertThat(reads[0]).isEqualTo(257);
    }

    @Test
    void targetAllowlistAndSecretValidationRunBeforeConstructingAPool() {
        for (String database : new String[]{"postgres", "authority_pro?user=private-input", "private-input", "", null}) {
            assertThatThrownBy(() -> LocalDatabase.runtimeDataSource(database, "private-input"))
                    .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
            assertThatThrownBy(() -> LocalDatabase.migrationDataSource(database, "private-input"))
                    .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
        }
        for (String password : new String[]{"", null, "x".repeat(257), "private-input\n", "private-input\r", "\0"}) {
            assertThatThrownBy(() -> LocalDatabase.createRuntime(password))
                    .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
            assertThatThrownBy(() -> LocalDatabase.migrate(password))
                    .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
        }
    }

    @Test
    void dependencyFailuresAreReplacedInsteadOfWrapped() throws Exception {
        var source = mock(DataSource.class);
        when(source.getConnection()).thenThrow(new SQLException("private-input SELECT secret", "28000"));
        assertThatThrownBy(() -> LocalDatabase.verifyRuntime(source))
                .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
        assertThatThrownBy(() -> LocalDatabase.flyway(source))
                .hasMessage("DATABASE_STARTUP_REFUSED").hasNoCause();
    }

    @Test
    void migrationCannotBeStartedAsASpringApplication() {
        assertThatThrownBy(() -> AuthorityProApplication.application(new SkeletonSettings(SkeletonSettings.Mode.MIGRATE_LOCAL)))
                .hasMessage("MIGRATION_CONTEXT_FORBIDDEN");
    }

    private static InputStream input(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}

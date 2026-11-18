package com.authoritypro.launch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class BuildBaselineTest {
    @Test
    void runsOnTheApprovedTargetJdk() {
        assertThat(Runtime.version().feature()).isEqualTo(21);
        assertThat(Runtime.version().update()).isEqualTo(13);
        assertThat(Runtime.version().build()).hasValue(9);
        assertThat(System.getProperty("java.vendor")).contains("Eclipse Adoptium");
    }

    @Test
    void wrapperPinsDistributionAndIntegrity() throws Exception {
        var properties = new Properties();
        try (var source = Files.newInputStream(Path.of(".mvn", "wrapper", "maven-wrapper.properties"))) {
            properties.load(source);
        }
        assertThat(properties.getProperty("wrapperVersion")).isEqualTo("3.3.4");
        assertThat(properties.getProperty("distributionType")).isEqualTo("only-script");
        assertThat(properties.getProperty("distributionUrl"))
                .isEqualTo("https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip");
        assertThat(properties.getProperty("distributionSha256Sum"))
                .isEqualTo("5af3b743dd8b876b5c45da33b676251e5f1687712644abb4ee519ca56e1d89ce");
    }
}

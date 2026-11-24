package com.authoritypro.platform.runtime.infrastructure.manifest;

import com.authoritypro.platform.runtime.application.BuildRevision;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Reads the build-injected revision; packaging always provides it, tests may see the fallback. */
public final class BuildRevisionReader {
    static final String LOCATION = "/authority-pro/build.properties";
    static final String FALLBACK = "UNRECORDED";

    private BuildRevisionReader() { }

    public static BuildRevision read() {
        Properties properties = new Properties();
        try (InputStream input = BuildRevisionReader.class.getResourceAsStream(LOCATION)) {
            if (input != null) properties.load(input);
        } catch (IOException unreadable) {
            return new BuildRevision(FALLBACK);
        }
        String revision = properties.getProperty("buildRevision", "").trim();
        return new BuildRevision(revision.matches("[A-Za-z0-9._-]{1,64}") ? revision : FALLBACK);
    }
}

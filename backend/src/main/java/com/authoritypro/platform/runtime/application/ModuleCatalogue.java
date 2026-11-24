package com.authoritypro.platform.runtime.application;

import java.util.List;

/** Immutable compiled catalogue; an unavailable catalogue is served as 503, never partially. */
public record ModuleCatalogue(List<Entry> modules, String unavailableReason) {

    public ModuleCatalogue {
        modules = List.copyOf(modules);
    }

    public record Entry(ModuleManifest manifest, String digest) { }

    public boolean available() {
        return unavailableReason == null;
    }

    public static ModuleCatalogue unavailable(String reason) {
        return new ModuleCatalogue(List.of(), reason);
    }
}

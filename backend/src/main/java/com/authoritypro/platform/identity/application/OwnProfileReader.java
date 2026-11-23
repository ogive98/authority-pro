package com.authoritypro.platform.identity.application;

import java.util.Optional;
import java.util.UUID;

/** Own-membership discovery: only the caller's active memberships and their labels are readable. */
public interface OwnProfileReader {
    Optional<OwnProfile> read(UUID userId);
}

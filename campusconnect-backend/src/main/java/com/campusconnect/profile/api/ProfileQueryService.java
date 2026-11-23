package com.campusconnect.profile.api;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ProfileQueryService {

    Optional<ProfileDetails> getDetails(long userId);

    /** ONE query for many users (feed N+1 fix, Phase D). Unknown ids are simply missing from the map. */
    Map<Long, ProfileSummary> getSummaries(Collection<Long> userIds);
}
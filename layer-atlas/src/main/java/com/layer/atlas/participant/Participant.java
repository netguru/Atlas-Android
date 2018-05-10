package com.layer.atlas.participant;

import android.support.annotation.Nullable;

import com.layer.sdk.messaging.Presence;

public interface Participant extends Comparable<Participant> {

    String getId();

    /**
     * @return First name + last name.
     */
    String getName();

    /**
     * @return jobTitle + companyName
     */
    String getJobDesc();

    String getFirstName();

    String getAvatarImageUrl();

    @Nullable
    Presence.PresenceStatus getPresenceStatus();

    void setPresenceStatus(Presence.PresenceStatus presenceStatus);
}

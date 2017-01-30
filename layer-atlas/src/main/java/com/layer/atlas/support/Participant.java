package com.layer.atlas.support;

import android.net.Uri;

public interface Participant extends Comparable<Participant> {
    /**
     * Returns the unique identifier for this Participant.
     *
     * @return The unique identifier for this Participant.
     */
    String getId();

    /**
     * Returns the name of this Participant.
     *
     * @return The name of this Participant.
     */
    String getName();

    /**
     * Returns the URL for an avatar image for this Participant.
     *
     * @return the URL for an avatar image for this Participant.
     */
    Uri getAvatarUrl();

    /**
     * Allows sorting Participants.
     */
    int compareTo(Participant another);
}

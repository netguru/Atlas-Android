package com.layer.atlas.participant;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.layer.sdk.messaging.Identity;
import com.layer.sdk.messaging.Presence;

public class BotParticipant implements Participant {

    private final String id;

    private final String firstName;

    private final String lastName;

    private final String name;

    private final String avatarUrl;

    private Presence.PresenceStatus presenceStatus;

    public static BotParticipant from(@NonNull Identity identity) {
        return new BotParticipant(identity.getUserId(),
                identity.getFirstName(),
                identity.getLastName(),
                identity.getDisplayName(),
                identity.getAvatarImageUrl());
    }

    private BotParticipant(final String id,
                           final String firstName,
                           final String lastName,
                           final String name,
                           final String avatarThumbnail) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.name = name;
        this.avatarUrl = avatarThumbnail;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getJobDesc() {
        return null;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getAvatarImageUrl() {
        return avatarUrl;
    }

    @Nullable
    @Override
    public Presence.PresenceStatus getPresenceStatus() {
        return presenceStatus;
    }

    @Override
    public void setPresenceStatus(Presence.PresenceStatus presenceStatus) {
        this.presenceStatus = presenceStatus;
    }

    @Override
    public int compareTo(@NonNull Participant another) {
        return id.compareTo(another.getId());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BotParticipant that = (BotParticipant) o;

        if (!id.equals(that.id)) return false;
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null)
            return false;
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null)
            return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (avatarUrl != null ? !avatarUrl.equals(that.avatarUrl) : that.avatarUrl != null)
            return false;
        return presenceStatus == that.presenceStatus;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (firstName != null ? firstName.hashCode() : 0);
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (avatarUrl != null ? avatarUrl.hashCode() : 0);
        result = 31 * result + (presenceStatus != null ? presenceStatus.hashCode() : 0);
        return result;
    }
}

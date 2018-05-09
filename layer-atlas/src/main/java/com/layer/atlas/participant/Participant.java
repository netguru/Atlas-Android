package com.layer.atlas.participant;

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
}

package com.layer.atlas.participant;


import android.support.annotation.NonNull;

import java.util.List;
import java.util.Map;

import io.reactivex.Single;

public interface ChatParticipantProvider {

    Single<List<Participant>> getParticipants(List<String> participantsIds);

    Single<Participant> getParticipant(String id);

    /**
     * Returns a map of all Participants by their unique ID who match the provided `filter`, or
     * all Participants if `filter` is `null`.  If `result` is provided, it is operated on and
     * returned.  If `result` is `null`, a new Map is created and returned.
     *
     * @param filter The filter to apply to Participants
     * @return A Single with Map of all matching Participants keyed by ID.
     */
    Single<Map<String, Participant>> getMatchingParticipants(@NonNull String filter);

    Single<List<Participant>> findParticipantsFromSameCompany(List<String> participantsIds);
}

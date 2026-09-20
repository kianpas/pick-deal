package com.pickdeal.collector.dto;

import java.util.List;

public final class CollectionResponses {
    private CollectionResponses() {}
    public record KnownIds(List<String> knownExternalIds, boolean hasCollectedDeals) {}
    public record Accepted(int received, int created, int updated) {}
}

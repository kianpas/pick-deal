package com.pickdeal.collector.remote;

import com.pickdeal.collector.support.CollectedDeal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/** DB 없는 파이프라인. 실패 배치는 메모리에 보존하고 다음 주기에 새 수집보다 먼저 보낸다. */
public final class RemoteCollectionRunner {
    public interface Source {
        String code();
        List<CollectedDeal> list(int page);
        CollectedDeal detail(CollectedDeal deal);
        default boolean supportsDetails() { return true; }
    }
    private final RemoteApiClient api;
    private final int bootstrapPages;
    private final int maxDetails;
    private final java.util.Map<String, List<CollectedDeal>> pending = new LinkedHashMap<>();

    public RemoteCollectionRunner(RemoteApiClient api, int bootstrapPages, int maxDetails) {
        if (bootstrapPages < 1 || bootstrapPages > 3 || maxDetails < 0 || maxDetails > 3) throw new IllegalArgumentException("Invalid request limits");
        this.api = api;
        this.bootstrapPages = bootstrapPages;
        this.maxDetails = maxDetails;
    }

    public void run(Source source) {
        if (pending.containsKey(source.code())) {
            api.send(source.code(), pending.get(source.code()));
            pending.remove(source.code());
            System.out.println("resent=[" + source.code() + "]");
            return;
        }
        var items = new LinkedHashMap<String, CollectedDeal>();
        add(items, source.list(1), 50);
        if (items.isEmpty()) throw new IllegalStateException("No valid list items");
        var known = api.known(source.code(), List.copyOf(items.keySet()));
        if (!known.hasCollectedDeals()) {
            for (int page = 2; page <= bootstrapPages && items.size() < 150; page++) {
                add(items, source.list(page), 150);
            }
            if (bootstrapPages > 1) known = api.known(source.code(), List.copyOf(items.keySet()));
        }
        List<CollectedDeal> enriched = new ArrayList<>();
        int requests = 0;
        for (var deal : items.values()) {
            if (source.supportsDetails() && !known.ids().contains(deal.externalId()) && requests < maxDetails) {
                requests++;
                try {
                    var detail = source.detail(deal);
                    String shop = bounded(detail.storeName(), 100);
                    var candidate = deal.withProductInfo(shop, safeUrl(detail.productUrl(), 2000));
                    deal = candidate.rawTitle().length() <= 300 ? candidate
                            : deal.withProductInfo(null, safeUrl(detail.productUrl(), 2000));
                }
                catch (RuntimeException failure) {
                    System.out.println("detailFailed=[" + source.code() + "]; using list data");
                }
            }
            enriched.add(deal);
        }
        pending.put(source.code(), List.copyOf(enriched));
        api.send(source.code(), enriched);
        pending.remove(source.code());
        System.out.println("sent=[" + source.code() + "]; items=" + enriched.size() + "; details=" + requests);
    }

    private static void add(LinkedHashMap<String, CollectedDeal> target, List<CollectedDeal> items, int limit) {
        for (var item : items) {
            if (target.size() >= limit) break;
            // 한 항목 때문에 배치 전체를 거부당하지 않도록 저장 계약 길이를 먼저 확인한다.
            if (item.externalId() == null || !item.externalId().matches("[1-9][0-9]{0,199}")
                    || item.title() == null || item.title().isBlank() || item.rawTitle().length() > 300
                    || item.url() == null || item.url().length() > 1000) continue;
            target.putIfAbsent(item.externalId(), new CollectedDeal(item.externalId(), item.url(), bounded(item.storeName(), 100),
                    item.title(), item.price(), bounded(item.category(), 50), item.commentCount(), safeUrl(item.thumbnailUrl(), 1000),
                    item.ended(), item.postedAt(), safeUrl(item.productUrl(), 2000)));
        }
    }
    private static String bounded(String value, int max) { return value == null || value.length() > max ? null : value; }
    private static String safeUrl(String value, int max) {
        if (bounded(value, max) == null) return null;
        try {
            var uri = java.net.URI.create(value);
            return ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null ? value : null;
        } catch (IllegalArgumentException ignored) { return null; }
    }
}

package com.pickdeal.collector.application;

import com.pickdeal.collector.dto.CollectionRequests;
import com.pickdeal.collector.dto.CollectionResponses;
import com.pickdeal.collector.support.CategoryNormalizer;
import com.pickdeal.collector.support.CollectedDeal;
import com.pickdeal.collector.support.DealUpsertSupport;
import com.pickdeal.common.error.BusinessException;
import com.pickdeal.common.error.ErrorCode;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.source.domain.SourceRepository;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RemoteCollectionService {
    private record SourceSpec(String name, String baseUrl) {}
    private static final Map<String, SourceSpec> SOURCES = Map.of(
            "quasarzone", new SourceSpec("퀘이사존", "https://quasarzone.com"),
            "ruliweb", new SourceSpec("루리웹", "https://bbs.ruliweb.com"),
            "ppomppu", new SourceSpec("뽐뿌", "https://www.ppomppu.co.kr"));
    private final SourceRepository sources;
    private final DealRepository deals;
    private final DealUpsertSupport upsert;
    private final TransactionTemplate transaction;

    public RemoteCollectionService(SourceRepository sources, DealRepository deals, DealUpsertSupport upsert,
            PlatformTransactionManager transactionManager) {
        this.sources = sources;
        this.deals = deals;
        this.upsert = upsert;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Transactional(readOnly = true)
    public CollectionResponses.KnownIds known(CollectionRequests.KnownIds request) {
        sourceSpec(request.sourceCode());
        return sources.findByCode(request.sourceCode()).map(source -> {
            if (!source.isActive()) throw badRequest("비활성 출처입니다.");
            var known = new HashSet<>(deals.findKnownExternalIds(source.getId(), request.externalIds()));
            return new CollectionResponses.KnownIds(request.externalIds().stream().distinct().filter(known::contains).toList(),
                    deals.existsBySourceId(source.getId()));
        }).orElseGet(() -> new CollectionResponses.KnownIds(List.of(), false));
    }

    /** 단일 서버의 수신 배치를 commit까지 직렬화한다. DB unique 제약도 유지한다. */
    public synchronized CollectionResponses.Accepted accept(CollectionRequests.Batch request) {
        var spec = sourceSpec(request.sourceCode());
        var ids = new HashSet<String>();
        var normalized = request.deals().stream().map(item -> {
            if (!ids.add(item.externalId())) throw badRequest("배치 내 externalId가 중복됩니다.");
            validateOriginalUrl(request.sourceCode(), spec, item);
            validateOptionalUrl(item.productUrl());
            validateOptionalUrl(item.thumbnailUrl());
            String shop = blankToNull(item.shopName());
            var deal = new CollectedDeal(item.externalId(), item.originalUrl(), shop, item.title(), item.price(),
                    CategoryNormalizer.normalize(item.category()), item.commentCount(), blankToNull(item.thumbnailUrl()),
                    item.ended(), item.postedAt(), blankToNull(item.productUrl()));
            if (deal.rawTitle().length() > 300) throw badRequest("판매처를 포함한 제목은 300자 이하여야 합니다.");
            return deal;
        }).toList();
        try {
            return transaction.execute(status -> {
                var source = upsert.findOrRegisterSource(request.sourceCode(), spec.name(), spec.baseUrl());
                if (!source.isActive()) throw badRequest("비활성 출처입니다.");
                int created = upsert.upsertAll(source, normalized, OffsetDateTime.now());
                deals.flush();
                return new CollectionResponses.Accepted(normalized.size(), created, normalized.size() - created);
            });
        } catch (DataIntegrityViolationException conflict) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "저장 충돌로 배치가 취소됐습니다. 수집기 중복 실행을 확인하세요.");
        }
    }

    private static SourceSpec sourceSpec(String code) {
        var spec = SOURCES.get(code);
        if (spec == null) throw badRequest("지원하지 않는 출처입니다.");
        return spec;
    }

    private static void validateOriginalUrl(String code, SourceSpec spec, CollectionRequests.Item item) {
        URI url = httpUri(item.originalUrl());
        if (!"https".equals(url.getScheme()) || !URI.create(spec.baseUrl()).getHost().equals(url.getHost())
                || url.getPort() != -1 || url.getFragment() != null) throw badRequest("원문 URL 출처가 일치하지 않습니다.");
        String expectedPath = switch (code) {
            case "quasarzone" -> "/bbs/qb_saleinfo/views/" + item.externalId();
            case "ruliweb" -> "/market/board/1020/read/" + item.externalId();
            default -> "/zboard/view.php";
        };
        if (!expectedPath.equals(url.getPath())) throw badRequest("원문 URL과 externalId가 일치하지 않습니다.");
        if (code.equals("ppomppu")) {
            String query = url.getRawQuery();
            if (!("id=ppomppu&no=" + item.externalId()).equals(query)) {
                throw badRequest("뽐뿌 원문 URL은 id=ppomppu&no=externalId 형식이어야 합니다.");
            }
        } else if (url.getRawQuery() != null && !url.getRawQuery().isEmpty()) {
            throw badRequest("원문 URL에 불필요한 쿼리를 포함할 수 없습니다.");
        }
    }

    private static void validateOptionalUrl(String value) {
        if (value != null && !value.isBlank()) httpUri(value);
    }

    private static URI httpUri(String value) {
        try {
            URI uri = URI.create(value);
            if (("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null) return uri;
        } catch (IllegalArgumentException ignored) { }
        throw badRequest("HTTP(S) URL만 허용합니다.");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BusinessException badRequest(String message) {
        return new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}

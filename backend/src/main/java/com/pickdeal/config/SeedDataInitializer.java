package com.pickdeal.config;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.keyword.domain.KeywordType;
import com.pickdeal.keyword.domain.Keyword;
import com.pickdeal.keyword.domain.KeywordRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import com.pickdeal.source.domain.SourceVisibility;
import com.pickdeal.source.domain.SourceVisibilityRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedDataInitializer {

    private static final Long DEFAULT_USER_ID = 1L;

    @Bean
    CommandLineRunner seedData(
            SourceRepository sourceRepository,
            DealRepository dealRepository,
            KeywordRepository keywordRepository,
            SourceVisibilityRepository sourceVisibilityRepository
    ) {
        return args -> {
            if (sourceRepository.count() > 0) {
                return;
            }

            Source exampleDeals = sourceRepository.save(new Source(
                    "Example Deals",
                    "https://example.com",
                    "example_deals",
                    true
            ));
            Source communityHotdeal = sourceRepository.save(new Source(
                    "Community Hotdeal",
                    "https://community.example.com",
                    "community_hotdeal",
                    true
            ));
            Source hiddenSample = sourceRepository.save(new Source(
                    "Hidden Sample",
                    "https://hidden.example.com",
                    "hidden_sample",
                    true
            ));

            sourceVisibilityRepository.save(new SourceVisibility(DEFAULT_USER_ID, hiddenSample, false));

            keywordRepository.saveAll(List.of(
                    new Keyword(DEFAULT_USER_ID, KeywordType.INTEREST, "마우스"),
                    new Keyword(DEFAULT_USER_ID, KeywordType.INTEREST, "키보드"),
                    new Keyword(DEFAULT_USER_ID, KeywordType.EXCLUDE, "리퍼"),
                    new Keyword(DEFAULT_USER_ID, KeywordType.EXCLUDE, "중고")
            ));

            OffsetDateTime now = OffsetDateTime.now();
            dealRepository.saveAll(List.of(
                    new Deal(
                            exampleDeals,
                            "무선 마우스 특가",
                            "수동 등록된 MVP 샘플 핫딜입니다. 가벼운 무선 마우스 할인 정보입니다.",
                            19900L,
                            29900L,
                            33,
                            "KRW",
                            "전자제품",
                            "https://example.com/thumbs/mouse.jpg",
                            "https://example.com/deals/1",
                            "example-1",
                            null,
                            DealStatus.ACTIVE,
                            now.minusHours(1),
                            now.minusHours(1)
                    ),
                    new Deal(
                            communityHotdeal,
                            "기계식 키보드 주말 할인",
                            "청축 기계식 키보드 할인 샘플 데이터입니다.",
                            59000L,
                            79000L,
                            25,
                            "KRW",
                            "전자제품",
                            "https://community.example.com/thumbs/keyboard.jpg",
                            "https://community.example.com/deals/keyboard-weekend",
                            "community-1",
                            null,
                            DealStatus.ACTIVE,
                            now.minusHours(3),
                            now.minusHours(3)
                    ),
                    new Deal(
                            communityHotdeal,
                            "리퍼 노트북 한정 판매",
                            "제외 키워드 필터링 확인용 샘플입니다.",
                            399000L,
                            499000L,
                            20,
                            "KRW",
                            "노트북",
                            "https://community.example.com/thumbs/refurb-laptop.jpg",
                            "https://community.example.com/deals/refurb-laptop",
                            "community-2",
                            null,
                            DealStatus.ACTIVE,
                            now.minusHours(5),
                            now.minusHours(5)
                    ),
                    new Deal(
                            hiddenSample,
                            "숨김 출처 샘플 핫딜",
                            "출처 표시 설정이 false인 경우 목록에서 제외됩니다.",
                            10000L,
                            15000L,
                            33,
                            "KRW",
                            "샘플",
                            "https://hidden.example.com/thumbs/hidden.jpg",
                            "https://hidden.example.com/deals/hidden-1",
                            "hidden-1",
                            null,
                            DealStatus.ACTIVE,
                            now.minusHours(2),
                            now.minusHours(2)
                    )
            ));
        };
    }
}

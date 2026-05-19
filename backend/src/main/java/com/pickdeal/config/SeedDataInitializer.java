package com.pickdeal.config;

import com.pickdeal.deal.domain.Deal;
import com.pickdeal.deal.domain.DealRepository;
import com.pickdeal.deal.domain.DealStatus;
import com.pickdeal.preference.domain.KeywordType;
import com.pickdeal.preference.domain.PreferenceKeyword;
import com.pickdeal.preference.domain.PreferenceKeywordRepository;
import com.pickdeal.source.domain.Source;
import com.pickdeal.source.domain.SourceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedDataInitializer {

    @Bean
    CommandLineRunner seedData(
            SourceRepository sourceRepository,
            DealRepository dealRepository,
            PreferenceKeywordRepository keywordRepository
    ) {
        return args -> {
            if (sourceRepository.count() > 0) {
                return;
            }

            Source exampleDeals = sourceRepository.save(new Source(
                    "Example Deals",
                    "https://example.com",
                    "개발용 샘플 출처",
                    true
            ));
            Source communityHotdeal = sourceRepository.save(new Source(
                    "Community Hotdeal",
                    "https://community.example.com",
                    "커뮤니티 핫딜 샘플 출처",
                    true
            ));
            Source hiddenSample = sourceRepository.save(new Source(
                    "Hidden Sample",
                    "https://hidden.example.com",
                    "숨김 처리된 샘플 출처",
                    false
            ));

            keywordRepository.saveAll(List.of(
                    new PreferenceKeyword(KeywordType.INTEREST, "마우스"),
                    new PreferenceKeyword(KeywordType.INTEREST, "키보드"),
                    new PreferenceKeyword(KeywordType.EXCLUDE, "리퍼"),
                    new PreferenceKeyword(KeywordType.EXCLUDE, "중고")
            ));

            OffsetDateTime now = OffsetDateTime.now();
            dealRepository.saveAll(List.of(
                    new Deal(
                            exampleDeals,
                            "무선 마우스 특가",
                            "수동 등록된 MVP 샘플 핫딜입니다. 가벼운 무선 마우스 할인 정보입니다.",
                            19900,
                            0,
                            "https://example.com/deals/1",
                            "example-1",
                            DealStatus.ACTIVE,
                            now.minusHours(1),
                            now.minusHours(1)
                    ),
                    new Deal(
                            communityHotdeal,
                            "기계식 키보드 주말 할인",
                            "청축 기계식 키보드 할인 샘플 데이터입니다.",
                            59000,
                            3000,
                            "https://community.example.com/deals/keyboard-weekend",
                            "community-1",
                            DealStatus.ACTIVE,
                            now.minusHours(3),
                            now.minusHours(3)
                    ),
                    new Deal(
                            communityHotdeal,
                            "리퍼 노트북 한정 판매",
                            "제외 키워드 필터링 확인용 샘플입니다.",
                            399000,
                            0,
                            "https://community.example.com/deals/refurb-laptop",
                            "community-2",
                            DealStatus.ACTIVE,
                            now.minusHours(5),
                            now.minusHours(5)
                    ),
                    new Deal(
                            hiddenSample,
                            "숨김 출처 샘플 핫딜",
                            "출처 표시 설정이 false인 경우 목록에서 제외됩니다.",
                            10000,
                            2500,
                            "https://hidden.example.com/deals/hidden-1",
                            "hidden-1",
                            DealStatus.ACTIVE,
                            now.minusHours(2),
                            now.minusHours(2)
                    )
            ));
        };
    }
}


package com.econoup.dailyconnect;

import com.econoup.curriculum.QuestionInteractionService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DailyContentSeeder {
    private static final List<ShortsSeed> SHORTS = List.of(
            new ShortsSeed("stock_market", "증시", "증권이 거래되는 시장 전체를 뜻해요.", "ab_8TU7-qtY"),
            new ShortsSeed("kospi", "코스피", "한국거래소 유가증권시장에 상장된 대표 주식들의 흐름을 보여주는 지수예요.", "rsSh_HcnlA4"),
            new ShortsSeed("leverage", "레버리지", "빌린 돈이나 파생상품을 활용해 투자 효과를 키우는 방식이에요.", "wBNMvj2NTSc"),
            new ShortsSeed("fund_amount", "기금액", "특정 목적을 위해 모아 두거나 운용하는 자금의 규모예요.", "e2Ce54WER54"),
            new ShortsSeed("transfer_tax_surcharge", "양도세 중과", "특정 자산을 팔 때 일반 세율보다 더 무겁게 양도소득세를 매기는 제도예요.", "pG6jw93zPpg"),
            new ShortsSeed("long_term_bond", "장기채", "만기가 긴 채권으로, 금리 변화에 가격이 더 민감하게 움직이는 편이에요.", "Si2YS5dAPmQ"),
            new ShortsSeed("sp500", "S&P 500", "미국 대표 대형주 500개 기업으로 구성된 주가지수예요.", "KX7uOEiPnN4"),
            new ShortsSeed("hedge_fund", "헤지펀드", "다양한 전략으로 수익을 추구하는 전문 투자 펀드예요.", "a4lMWzaTEUQ"),
            new ShortsSeed("k_shaped_polarization", "K자 양극화", "경기 회복 과정에서 일부는 좋아지고 일부는 나빠지는 격차 확대 현상이에요.", "-51dpSOcMpo"),
            new ShortsSeed("separate_taxation", "분리과세", "특정 소득을 다른 소득과 합산하지 않고 별도로 세금을 매기는 방식이에요.", "TFmHlnb4eAM"),
            new ShortsSeed("mou", "MOU", "기관이나 기업이 협력 의사를 확인하기 위해 맺는 양해각서예요.", "AKF0kxAeevk"),
            new ShortsSeed("mid_term_asset_allocation", "중기자산배분", "중기 투자 기간을 기준으로 자산 비중을 나누는 운용 전략이에요.", "Qw3OUyktx6c")
    );

    private final DailyArticleRepository articleRepository;
    private final ArticleBookmarkRepository bookmarkRepository;
    private final DailyQuizAnswerRepository quizAnswerRepository;
    private final TermRepository termRepository;
    private final QuestionInteractionService json;

    public DailyContentSeeder(
            DailyArticleRepository articleRepository,
            ArticleBookmarkRepository bookmarkRepository,
            DailyQuizAnswerRepository quizAnswerRepository,
            TermRepository termRepository,
            QuestionInteractionService json
    ) {
        this.articleRepository = articleRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.termRepository = termRepository;
        this.json = json;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        removeLegacyPlaceholder();
        Instant firstPublishedAt = Instant.parse("2026-06-23T00:00:00Z");
        for (int index = 0; index < SHORTS.size(); index++) {
            ShortsSeed seed = SHORTS.get(index);
            upsertTerm(seed);
            upsertArticle(seed, firstPublishedAt.minusSeconds(index * 60L));
        }
    }

    private void removeLegacyPlaceholder() {
        String legacyId = "news_rate_hold_001";
        if (!articleRepository.existsById(legacyId)) {
            return;
        }
        quizAnswerRepository.deleteByArticle_Id(legacyId);
        bookmarkRepository.deleteByArticle_Id(legacyId);
        articleRepository.deleteById(legacyId);
    }

    private void upsertTerm(ShortsSeed seed) {
        TermEntity term = termRepository.findById(seed.termId())
                .orElseGet(() -> new TermEntity(seed.termId(), seed.term(), seed.definition(), 1L));
        term.name = seed.term();
        term.definition = seed.definition();
        term.relatedStageId = 1L;
        termRepository.save(term);
    }

    private void upsertArticle(ShortsSeed seed, Instant publishedAt) {
        DailyArticleEntity article = articleRepository.findById(seed.articleId()).orElseGet(DailyArticleEntity::new);
        article.id = seed.articleId();
        article.categoryCode = "ECONOMY";
        article.title = seed.term();
        article.subtitle = "오늘의 경제 용어";
        article.summaryJson = json.writeJson(List.of(
                seed.term() + " 개념을 짧은 영상으로 학습합니다.",
                "카드 썸네일을 누르면 유튜브 Shorts 영상으로 연결할 수 있습니다.",
                "시청 후 퀴즈로 핵심 용어를 확인합니다."
        ));
        article.body = seed.definition() + "\n\n영상 링크: " + seed.youtubeUrl();
        article.sourceName = "Econo-up Shorts";
        article.sourceUrl = seed.youtubeUrl();
        article.youtubeVideoId = seed.youtubeVideoId();
        article.termIdsJson = json.writeJson(List.of(seed.termId()));
        article.relatedStageId = 1L;
        article.quizPrompt = "이 영상에서 학습하는 핵심 용어는 무엇인가요?";
        article.quizChoicesJson = json.writeJson(List.of(
                Map.of("id", "A", "text", seed.term()),
                Map.of("id", "B", "text", "기준금리"),
                Map.of("id", "C", "text", "환율")
        ));
        article.quizCorrectChoiceId = "A";
        article.quizExplanation = "이 콘텐츠의 핵심 학습 용어는 '" + seed.term() + "'입니다.";
        article.publishedAt = publishedAt;
        articleRepository.save(article);
    }

    private record ShortsSeed(String termId, String term, String definition, String youtubeVideoId) {
        String articleId() {
            return "daily_short_" + termId;
        }

        String youtubeUrl() {
            return "https://www.youtube.com/shorts/" + youtubeVideoId;
        }
    }
}

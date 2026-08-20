package com.example.hackathoncodaro2026.intent.service;

import com.example.hackathoncodaro2026.intent.model.IntentSpec;
import com.example.hackathoncodaro2026.intent.model.TimeOfDay;
import com.example.hackathoncodaro2026.intent.parse.IntentParser;

import java.time.LocalDate;
import java.util.List;

/**
 * TEMPORARY — replaced by {@code com.example.hackathoncodaro2026.intent.parse.RuleIntentParser}
 * once that class lands (it is being written in parallel). Swapping is a
 * one-line change in {@link IntentBeansConfig#intentParser()}.
 *
 * Never throws, per the {@link IntentParser} contract: with no real language
 * understanding available, it returns the most conservative spec it can
 * defend — a one hour slot, any time of day, over the next 7 days, no
 * resource type or constraints — and is honest about it via
 * {@code parserUsed = "rules-stub"} so callers can tell this apart from the
 * real "rules" or "llm" parser.
 */
public final class StubIntentParser implements IntentParser {

    private static final int DEFAULT_DURATION_MIN = 60;
    private static final int DEFAULT_WINDOW_DAYS = 7;

    @Override
    public ParseResult parse(String text, LocalDate today, int partySize) {
        LocalDate from = today == null ? LocalDate.now() : today;
        LocalDate to = from.plusDays(DEFAULT_WINDOW_DAYS);
        IntentSpec spec = new IntentSpec(
                DEFAULT_DURATION_MIN,
                from,
                to,
                TimeOfDay.ANY,
                List.of(),
                List.of(),
                null,
                partySize
        );
        return new ParseResult(spec, "rules-stub");
    }
}

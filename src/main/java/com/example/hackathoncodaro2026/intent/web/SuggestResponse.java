package com.example.hackathoncodaro2026.intent.web;

import java.util.List;

/** {@code POST /api/intent/suggest} response body. */
public record SuggestResponse(
        IntentSpecDto spec,
        String parserUsed,
        List<SuggestionDto> suggestions,
        List<RelaxStepDto> relaxationTrail
) {
}

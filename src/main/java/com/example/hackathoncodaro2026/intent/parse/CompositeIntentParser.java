package com.example.hackathoncodaro2026.intent.parse;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * The adapter every caller actually talks to. Prefers {@link
 * CodaroIntentParser} when it is configured, and falls back to {@link
 * RuleIntentParser} on any exception, timeout, or invalid result from it.
 *
 * Missing LLM credentials is a normal operating mode, not an error: this
 * class does not log a warning or throw for that case, it simply runs the
 * rule parser. The caller only learns which parser ran through {@code
 * parserUsed} on the result.
 */
@Component
@Primary
public class CompositeIntentParser implements IntentParser {

    private final CodaroIntentParser llmParser;
    private final RuleIntentParser ruleParser;

    public CompositeIntentParser(CodaroIntentParser llmParser, RuleIntentParser ruleParser) {
        this.llmParser = llmParser;
        this.ruleParser = ruleParser;
    }

    @Override
    public ParseResult parse(String text, LocalDate today, int partySize) {
        if (llmParser.isConfigured()) {
            try {
                return llmParser.parse(text, today, partySize);
            } catch (Exception e) {
                // Any failure of the optional upgrade — network, timeout,
                // malformed response, invalid result — falls back to the
                // deterministic parser. The caller never sees this exception.
            }
        }
        return ruleParser.parse(text, today, partySize);
    }
}

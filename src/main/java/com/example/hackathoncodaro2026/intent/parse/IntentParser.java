package com.example.hackathoncodaro2026.intent.parse;

import com.example.hackathoncodaro2026.intent.model.IntentSpec;

import java.time.LocalDate;

/**
 * Natural language in, {@link IntentSpec} out. The parser is the ONLY place a
 * language model may be used: it never ranks, never scores, never chooses a
 * slot. That division is what makes the recommendations explainable and what
 * keeps the product working when the model is unreachable.
 *
 * Implementations must never throw for unparseable input — return the most
 * conservative spec they can defend and say which parser produced it.
 */
public interface IntentParser {

    ParseResult parse(String text, LocalDate today, int partySize);

    /** {@code parserUsed} is surfaced to the client and logged: "rules" or "llm". */
    record ParseResult(IntentSpec spec, String parserUsed) {
    }
}

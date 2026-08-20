package com.example.hackathoncodaro2026.intent.service;

import com.example.hackathoncodaro2026.intent.engine.SlotRanker;
import com.example.hackathoncodaro2026.intent.parse.IntentParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the pluggable pieces of the intent pipeline. Both beans below are
 * TEMPORARY stubs (see {@link StubSlotRanker} / {@link StubIntentParser}):
 * {@code intent.engine.DefaultSlotRanker} and {@code intent.parse.RuleIntentParser}
 * are being written in parallel by other agents and did not exist in this
 * package set at the time this file was written. Once they land, each bean
 * method below is a one-line swap — no other code in this package needs to
 * change because everything downstream is coded against the {@link SlotRanker}
 * and {@link IntentParser} interfaces.
 */
@Configuration
public class IntentBeansConfig {

    // TEMPORARY — swap to: new com.example.hackathoncodaro2026.intent.engine.DefaultSlotRanker()
    @Bean
    public SlotRanker slotRanker() {
        return new StubSlotRanker();
    }

    // TEMPORARY — swap to: new com.example.hackathoncodaro2026.intent.parse.RuleIntentParser(...)
    @Bean
    public IntentParser intentParser() {
        return new StubIntentParser();
    }
}

package com.example.hackathoncodaro2026.intent.service;

import com.example.hackathoncodaro2026.intent.engine.DefaultSlotRanker;
import com.example.hackathoncodaro2026.intent.engine.SlotRanker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the ranking engine into the Spring context.
 *
 * The ranker is declared here as a plain {@code new} rather than annotated as a
 * component, because {@code intent.engine} is deliberately free of Spring: it is
 * a pure function over a snapshot, which is what lets it be unit-tested without
 * a context or a database. This class is the single place that dependency is
 * turned into a bean.
 *
 * The parser needs no bean method — {@code intent.parse.CompositeIntentParser}
 * is a {@code @Primary @Component} that already selects the LLM parser when it
 * is configured and falls back to the deterministic rule parser otherwise.
 */
@Configuration
public class IntentBeansConfig {

    @Bean
    public SlotRanker slotRanker() {
        return new DefaultSlotRanker();
    }
}

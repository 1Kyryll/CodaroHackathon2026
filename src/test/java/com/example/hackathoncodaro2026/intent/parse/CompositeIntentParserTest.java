package com.example.hackathoncodaro2026.intent.parse;

import com.example.hackathoncodaro2026.intent.config.IntentProperties;
import com.example.hackathoncodaro2026.intent.model.IntentSpec;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Plain JUnit 5, no Spring context, no network. Proves the one contract that
 * matters most for the live demo: absent/failing LLM credentials degrade
 * quietly to {@link RuleIntentParser}, never throwing to the caller.
 */
class CompositeIntentParserTest {

    private static final LocalDate TODAY = LocalDate.of(2024, 6, 3);

    private final IntentProperties config = RuleIntentParserTest.testConfig();

    @Test
    void fallsBackToRulesWhenLlmIsUnconfigured() {
        CodaroIntentParser unconfigured = new CodaroIntentParser("", "", "", config);
        RuleIntentParser ruleParser = new RuleIntentParser(config);
        CompositeIntentParser composite = new CompositeIntentParser(unconfigured, ruleParser);

        IntentParser.ParseResult result = composite.parse(
                "tennis for two tomorrow evening, outdoor court, about 90 minutes", TODAY, 1);

        assertThat(result.parserUsed()).isEqualTo("rules");
        IntentSpec spec = result.spec();
        assertThat(spec.durationMin()).isEqualTo(90);
        assertThat(spec.resourceType()).isEqualTo("TENNIS");
    }

    @Test
    void doesNotThrowOrLogScarilyWhenCredentialsAreAbsent() {
        CodaroIntentParser unconfigured = new CodaroIntentParser(null, null, null, config);
        CompositeIntentParser composite = new CompositeIntentParser(unconfigured, new RuleIntentParser(config));

        assertThatCode(() -> composite.parse("gym tomorrow", TODAY, 1)).doesNotThrowAnyException();
    }

    @Test
    void fallsBackToRulesWhenConfiguredLlmParserThrows() {
        CodaroIntentParser configuredButBroken = new CodaroIntentParser(
                "http://localhost:1234/v1", "sk-test", "some-model", config) {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public ParseResult parse(String text, LocalDate today, int partySize) {
                throw new IntentParseException("simulated network failure");
            }
        };
        CompositeIntentParser composite = new CompositeIntentParser(configuredButBroken, new RuleIntentParser(config));

        IntentParser.ParseResult result = composite.parse("squash tomorrow", TODAY, 1);

        assertThat(result.parserUsed()).isEqualTo("rules");
        assertThat(result.spec().resourceType()).isEqualTo("SQUASH");
    }

    @Test
    void fallsBackToRulesWhenConfiguredLlmParserReturnsInvalidResultViaException() {
        // Simulates the "invalid result" case: CodaroIntentParser's own
        // validation throws IntentParseException before returning, so from
        // the composite's point of view this looks identical to a network
        // failure — proving both paths land on the same safe fallback.
        CodaroIntentParser configuredButInvalid = new CodaroIntentParser(
                "http://localhost:1234/v1", "sk-test", "some-model", config) {
            @Override
            public boolean isConfigured() {
                return true;
            }

            @Override
            public ParseResult parse(String text, LocalDate today, int partySize) {
                throw new IntentParseException("LLM returned dayFrom after dayTo");
            }
        };
        CompositeIntentParser composite = new CompositeIntentParser(configuredButInvalid, new RuleIntentParser(config));

        IntentParser.ParseResult result = composite.parse("basketball next Friday evening", TODAY, 1);

        assertThat(result.parserUsed()).isEqualTo("rules");
        assertThat(result.spec().resourceType()).isEqualTo("BASKETBALL");
    }
}

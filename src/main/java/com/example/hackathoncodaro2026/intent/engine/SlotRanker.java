package com.example.hackathoncodaro2026.intent.engine;

import com.example.hackathoncodaro2026.intent.config.IntentProperties;
import com.example.hackathoncodaro2026.intent.model.IntentSpec;
import com.example.hackathoncodaro2026.intent.model.RankResult;
import com.example.hackathoncodaro2026.intent.model.ScheduleSnapshot;

/**
 * The contract every work package agrees on.
 *
 * Implementations MUST be pure: no repository, no clock, no Spring, no JPA.
 * Given the same snapshot, spec and config, the same result — every time.
 * That property is what lets the ranking logic be unit-tested in milliseconds
 * and is why it cannot fail during a live demo.
 */
public interface SlotRanker {

    RankResult rank(IntentSpec spec, ScheduleSnapshot snapshot, IntentProperties config);
}

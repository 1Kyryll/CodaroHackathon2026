package com.example.hackathoncodaro2026.intent.parse;

/**
 * Internal signal used by {@link CodaroIntentParser} to tell {@link
 * CompositeIntentParser} "this attempt didn't produce a trustworthy spec" —
 * covers network failure, timeout, malformed JSON, and results that fail
 * validation against {@link com.example.hackathoncodaro2026.intent.config.IntentProperties}.
 *
 * Never escapes {@link CompositeIntentParser}: the composite always falls
 * back to {@link RuleIntentParser} instead of letting this propagate to the
 * caller, honoring {@link IntentParser}'s "never throw" contract at the
 * public seam.
 */
public class IntentParseException extends RuntimeException {

    public IntentParseException(String message) {
        super(message);
    }

    public IntentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

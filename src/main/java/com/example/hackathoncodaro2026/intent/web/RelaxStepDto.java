package com.example.hackathoncodaro2026.intent.web;

import java.util.List;

/** JSON view of {@link com.example.hackathoncodaro2026.intent.model.RelaxStep}. */
public record RelaxStepDto(String action, String detail, List<String> droppedKeys) {
}

package com.example.hackathoncodaro2026.service;

import com.example.hackathoncodaro2026.model.SportSkillLevel;
import com.example.hackathoncodaro2026.model.enums.ResourceType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class SportSkillLevelCatalog {

    private final Map<ResourceType, List<SportSkillLevel>> levelsBySport = new EnumMap<>(ResourceType.class);

    public SportSkillLevelCatalog() {
        levelsBySport.put(ResourceType.TENNIS, ntrp());
        levelsBySport.put(ResourceType.SQUASH, squashGrades());
        levelsBySport.put(ResourceType.FOOTBALL, footballLadder());
        levelsBySport.put(ResourceType.BASKETBALL, basketballLadder());
        levelsBySport.put(ResourceType.VOLLEYBALL, volleyballUsav());
        levelsBySport.put(ResourceType.GYM, gymFitness());
        levelsBySport.put(ResourceType.SWIMMING, swimEngland());
    }

    public List<SportSkillLevel> levelsFor(ResourceType sport) {
        if (sport == null) {
            return List.of();
        }
        return levelsBySport.getOrDefault(sport, List.of());
    }

    public Map<String, List<SportSkillLevel>> optionsBySport() {
        Map<String, List<SportSkillLevel>> options = new LinkedHashMap<>();
        for (ResourceType sport : ResourceType.values()) {
            options.put(sport.name(), levelsFor(sport));
        }
        return options;
    }

    public boolean isValid(ResourceType sport, String code) {
        if (sport == null || code == null || code.isBlank()) {
            return false;
        }
        String needle = code.trim();
        return levelsFor(sport).stream().anyMatch(level -> level.getCode().equals(needle));
    }

    public String label(ResourceType sport, String code) {
        if (sport == null || code == null || code.isBlank()) {
            return "";
        }
        String needle = code.trim();
        return levelsFor(sport).stream()
                .filter(level -> level.getCode().equals(needle))
                .map(SportSkillLevel::getLabel)
                .findFirst()
                .orElse(needle);
    }

    public String joinedLabels(ResourceType sport, Collection<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return "";
        }
        return codes.stream()
                .filter(code -> code != null && !code.isBlank())
                .map(code -> label(sport, code))
                .collect(Collectors.joining(", "));
    }

    public Set<String> codesFor(ResourceType sport) {
        return levelsFor(sport).stream().map(SportSkillLevel::getCode).collect(Collectors.toSet());
    }

    public String groupLabel(ResourceType sport) {
        if (sport == null) {
            return "Level";
        }
        return switch (sport) {
            case TENNIS -> "NTRP / level";
            case SQUASH -> "England grade";
            case FOOTBALL -> "Playing level";
            case BASKETBALL -> "Playing level";
            case VOLLEYBALL -> "USAV rating";
            case GYM -> "Fitness level";
            case SWIMMING -> "Swim England stage";
        };
    }

    private List<SportSkillLevel> ntrp() {
        List<SportSkillLevel> levels = new ArrayList<>();
        for (int tenths = 10; tenths <= 70; tenths += 5) {
            int whole = tenths / 10;
            int frac = tenths % 10;
            String code = whole + "." + frac;
            levels.add(new SportSkillLevel(code, "NTRP " + code));
        }
        return List.copyOf(levels);
    }

    private List<SportSkillLevel> squashGrades() {
        return List.of(
                new SportSkillLevel("G", "G · beginner"),
                new SportSkillLevel("F", "F"),
                new SportSkillLevel("E", "E"),
                new SportSkillLevel("D", "D"),
                new SportSkillLevel("C", "C"),
                new SportSkillLevel("B", "B"),
                new SportSkillLevel("A", "A"),
                new SportSkillLevel("OPEN", "Open")
        );
    }

    private List<SportSkillLevel> footballLadder() {
        return List.of(
                new SportSkillLevel("RECREATIONAL", "Recreational"),
                new SportSkillLevel("CLUB_AMATEUR", "Club amateur"),
                new SportSkillLevel("COUNTY_REGIONAL", "County / regional"),
                new SportSkillLevel("SEMI_PRO", "Semi-pro")
        );
    }

    private List<SportSkillLevel> basketballLadder() {
        return List.of(
                new SportSkillLevel("RECREATIONAL", "Recreational"),
                new SportSkillLevel("HIGH_SCHOOL_CLUB", "High school / club"),
                new SportSkillLevel("COLLEGIATE_AMATEUR", "Collegiate amateur"),
                new SportSkillLevel("COMPETITIVE", "Competitive")
        );
    }

    private List<SportSkillLevel> volleyballUsav() {
        return List.of(
                new SportSkillLevel("D", "D"),
                new SportSkillLevel("C", "C"),
                new SportSkillLevel("B", "B"),
                new SportSkillLevel("A", "A"),
                new SportSkillLevel("AA", "AA"),
                new SportSkillLevel("OPEN", "Open")
        );
    }

    private List<SportSkillLevel> gymFitness() {
        return List.of(
                new SportSkillLevel("BEGINNER", "Beginner"),
                new SportSkillLevel("INTERMEDIATE", "Intermediate"),
                new SportSkillLevel("ADVANCED", "Advanced")
        );
    }

    private List<SportSkillLevel> swimEngland() {
        List<SportSkillLevel> levels = new ArrayList<>();
        for (int stage = 1; stage <= 7; stage++) {
            levels.add(new SportSkillLevel("STAGE_" + stage, "Stage " + stage));
        }
        levels.add(new SportSkillLevel("CLUB", "Club / competitive"));
        return List.copyOf(levels);
    }
}

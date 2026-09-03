package com.kji.normalize;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class DescriptionSections {

    private static final int MAX_HEADING_LENGTH = 80;
    private static final int MAX_BULLETS_PER_SECTION = 40;

    private final List<Lexicon.SectionHeading> headings;

    public DescriptionSections(Lexicon lexicon) {
        this.headings = lexicon.sectionHeadings();
    }

    public Sections split(String description) {
        Map<Section, List<String>> collected = new EnumMap<>(Section.class);
        for (Section section : Section.values()) {
            collected.put(section, new ArrayList<>());
        }
        if (TextNormalizer.isBlank(description)) {
            return new Sections(collected);
        }

        Section current = Section.OTHER;
        for (String rawLine : description.lines().toList()) {
            String line = TextNormalizer.collapseWhitespace(rawLine);
            if (line == null || line.isEmpty()) {
                continue;
            }
            Section heading = headingFor(line);
            if (heading != null) {
                current = heading;
                continue;
            }
            List<String> bucket = collected.get(current);
            if (bucket.size() < MAX_BULLETS_PER_SECTION) {
                bucket.add(stripBullet(line));
            }
        }
        return new Sections(collected);
    }

    private Section headingFor(String line) {
        if (line.length() > MAX_HEADING_LENGTH) {
            return null;
        }
        String lowered = line.toLowerCase(Locale.ROOT);
        Section best = null;
        int bestLength = 0;
        for (Lexicon.SectionHeading heading : headings) {
            for (String term : heading.terms()) {
                String candidate = term.toLowerCase(Locale.ROOT);
                if (candidate.length() > bestLength && lowered.contains(candidate)) {
                    best = Section.valueOf(heading.section());
                    bestLength = candidate.length();
                }
            }
        }
        return best;
    }

    private String stripBullet(String line) {
        String stripped = line;
        while (!stripped.isEmpty() && isBulletMarker(stripped.charAt(0))) {
            stripped = stripped.substring(1).trim();
        }
        return stripped;
    }

    private boolean isBulletMarker(char candidate) {
        return candidate == '-' || candidate == '*' || candidate == 0x2022
                || candidate == 0x00b7 || candidate == 0x25cf || candidate == 0x25a0;
    }

    public enum Section {
        RESPONSIBILITIES,
        REQUIREMENTS,
        PREFERRED,
        OTHER
    }

    public record Sections(Map<Section, List<String>> bySection) {

        public List<String> get(Section section) {
            return bySection.getOrDefault(section, List.of());
        }

        public String textOf(Section section) {
            return String.join("\n", get(section));
        }

        public boolean hasStructuredSections() {
            return !get(Section.REQUIREMENTS).isEmpty()
                    || !get(Section.PREFERRED).isEmpty()
                    || !get(Section.RESPONSIBILITIES).isEmpty();
        }
    }
}

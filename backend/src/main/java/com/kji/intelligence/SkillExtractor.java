package com.kji.intelligence;

import com.kji.normalize.DescriptionSections;
import com.kji.normalize.TextNormalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SkillExtractor {

    private static final Pattern ASCII_ALIAS = Pattern.compile("^[a-z0-9][a-z0-9 .+#/_-]*$");
    private static final int EVIDENCE_WINDOW = 90;

    private final SkillRepository skillRepository;
    private volatile List<CompiledAlias> aliases = List.of();

    public SkillExtractor(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    @Transactional(readOnly = true)
    public List<Detection> extract(DescriptionSections.Sections sections, String title) {
        List<CompiledAlias> compiled = compiledAliases();
        Map<String, Detection> best = new LinkedHashMap<>();

        scan(compiled, title, JobSkill.RequirementLevel.MENTIONED, 0.70d, best);
        scan(compiled, sections.textOf(DescriptionSections.Section.PREFERRED),
                JobSkill.RequirementLevel.PREFERRED, 0.85d, best);
        scan(compiled, sections.textOf(DescriptionSections.Section.REQUIREMENTS),
                JobSkill.RequirementLevel.REQUIRED, 0.90d, best);
        scan(compiled, sections.textOf(DescriptionSections.Section.RESPONSIBILITIES),
                JobSkill.RequirementLevel.MENTIONED, 0.75d, best);
        scan(compiled, sections.textOf(DescriptionSections.Section.OTHER),
                JobSkill.RequirementLevel.MENTIONED, 0.65d, best);

        return List.copyOf(best.values());
    }

    private void scan(List<CompiledAlias> compiled, String text,
                      JobSkill.RequirementLevel level, double confidence,
                      Map<String, Detection> best) {
        if (TextNormalizer.isBlank(text)) {
            return;
        }
        String lowered = TextNormalizer.compatibilityNormalize(text).toLowerCase(Locale.ROOT);

        for (CompiledAlias alias : compiled) {
            int index = indexOf(alias, lowered);
            if (index < 0) {
                continue;
            }
            Detection existing = best.get(alias.slug());
            if (existing != null && rank(existing.level()) >= rank(level)) {
                continue;
            }
            best.put(alias.slug(), new Detection(alias.slug(), alias.category(), level,
                    confidence, evidence(text, lowered, index, alias.alias().length())));
        }
    }

    private int indexOf(CompiledAlias alias, String lowered) {
        if (alias.pattern() != null) {
            Matcher matcher = alias.pattern().matcher(lowered);
            return matcher.find() ? matcher.start() : -1;
        }
        return lowered.indexOf(alias.alias());
    }

    private String evidence(String original, String lowered, int index, int aliasLength) {
        int start = Math.max(0, index - EVIDENCE_WINDOW / 3);
        int end = Math.min(lowered.length(), index + aliasLength + EVIDENCE_WINDOW);
        String slice = original.length() >= end ? original.substring(start, end) : lowered.substring(start, end);
        return TextNormalizer.collapseWhitespace(slice);
    }

    private int rank(JobSkill.RequirementLevel level) {
        return switch (level) {
            case REQUIRED -> 3;
            case PREFERRED -> 2;
            case MENTIONED -> 1;
        };
    }

    private List<CompiledAlias> compiledAliases() {
        List<CompiledAlias> current = aliases;
        if (!current.isEmpty()) {
            return current;
        }
        List<CompiledAlias> built = new ArrayList<>();
        for (Skill skill : skillRepository.findAll()) {
            for (String rawAlias : skill.getAliases()) {
                String alias = rawAlias.toLowerCase(Locale.ROOT).trim();
                if (alias.isEmpty()) {
                    continue;
                }
                Pattern pattern = ASCII_ALIAS.matcher(alias).matches()
                        ? Pattern.compile("(?<![a-z0-9+#])" + Pattern.quote(alias) + "(?![a-z0-9+#])")
                        : null;
                built.add(new CompiledAlias(skill.getSlug(), skill.getCategory(), alias, pattern));
            }
        }
        built.sort((left, right) -> Integer.compare(right.alias().length(), left.alias().length()));
        aliases = List.copyOf(built);
        return aliases;
    }

    public void invalidateCache() {
        aliases = List.of();
    }

    public record Detection(String skillSlug, Skill.Category category,
                            JobSkill.RequirementLevel level, double confidence, String evidence) {
    }

    private record CompiledAlias(String slug, Skill.Category category, String alias, Pattern pattern) {
    }
}

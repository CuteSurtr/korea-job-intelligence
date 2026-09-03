package com.kji;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ModuleBoundaryTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java", "com", "kji");
    private static final Pattern IMPORT =
            Pattern.compile("^import\\s+com\\.kji\\.([a-z]+)\\.", Pattern.MULTILINE);
    private static final Pattern QUALIFIED_REFERENCE =
            Pattern.compile("(?<!import )\\bcom\\.kji\\.([a-z]+)\\.");

    private static final Map<String, Set<String>> ALLOWED = Map.ofEntries(
            Map.entry("common", Set.of()),
            Map.entry("config", Set.of()),
            Map.entry("normalize", Set.of("common")),
            Map.entry("snapshot", Set.of("common")),
            Map.entry("source", Set.of("common", "config", "normalize")),
            Map.entry("company", Set.of("common", "normalize")),
            Map.entry("job", Set.of("common", "config", "company")),
            Map.entry("dedupe", Set.of("common", "config", "job")),
            Map.entry("search", Set.of("common", "company", "job", "source")),
            Map.entry("intelligence", Set.of("common", "normalize")),
            Map.entry("scoring", Set.of("common", "normalize")),
            Map.entry("ingest", Set.of("common", "config", "normalize", "source", "snapshot",
                    "company", "job", "dedupe", "intelligence", "scoring")),
            Map.entry("web", Set.of("common", "config", "normalize", "source", "snapshot",
                    "company", "job", "dedupe", "ingest", "search", "intelligence", "scoring")));

    @Test
    @DisplayName("no module imports a module it is not allowed to depend on")
    void modulesRespectTheirDeclaredDependencies() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles()) {
            String module = moduleOf(file);
            if (module == null) {
                continue;
            }
            Set<String> allowed = ALLOWED.get(module);
            assertThat(allowed)
                    .as("module %s has no declared dependency rule", module)
                    .isNotNull();

            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (Pattern pattern : List.of(IMPORT, QUALIFIED_REFERENCE)) {
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String referenced = matcher.group(1);
                    if (referenced.equals(module) || allowed.contains(referenced)) {
                        continue;
                    }
                    violations.add(module + " -> " + referenced + " (" + file.getFileName() + ")");
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("nothing outside web depends on web")
    void nothingDependsOnWeb() throws IOException {
        List<String> violations = new ArrayList<>();

        for (Path file : javaFiles()) {
            String module = moduleOf(file);
            if (module == null || module.equals("web")) {
                continue;
            }
            if (Files.readString(file, StandardCharsets.UTF_8).contains("import com.kji.web.")) {
                violations.add(module + "/" + file.getFileName());
            }
        }

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("source adapters produce only raw records and never reach into the domain")
    void adaptersDoNotDependOnTheDomain() throws IOException {
        List<String> violations = new ArrayList<>();
        Set<String> forbidden = Set.of("com.kji.job.", "com.kji.company.", "com.kji.dedupe.",
                "com.kji.ingest.", "com.kji.search.");

        for (Path file : javaFiles()) {
            if (!"source".equals(moduleOf(file))) {
                continue;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String forbiddenImport : forbidden) {
                if (content.contains("import " + forbiddenImport)) {
                    violations.add(file.getFileName() + " -> " + forbiddenImport);
                }
            }
        }

        assertThat(violations).isEmpty();
    }

    private List<Path> javaFiles() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private String moduleOf(Path file) {
        Path relative = SOURCE_ROOT.relativize(file);
        return relative.getNameCount() < 2 ? null : relative.getName(0).toString();
    }
}

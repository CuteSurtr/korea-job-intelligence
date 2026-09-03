package com.kji.normalize;

import com.kji.normalize.Lexicon.LocationAlias;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class LocationNormalizer {

    private final List<LocationAlias> aliases;

    public LocationNormalizer(Lexicon lexicon) {
        this.aliases = lexicon.locationCityAliases();
    }

    public NormalizedLocation normalize(String rawLocation) {
        if (TextNormalizer.isBlank(rawLocation)) {
            return new NormalizedLocation(null, null, null, null);
        }
        String cleaned = TextNormalizer.collapseWhitespace(
                TextNormalizer.compatibilityNormalize(rawLocation));
        String lowered = cleaned.toLowerCase(Locale.ROOT);

        for (LocationAlias alias : aliases) {
            for (String term : alias.terms()) {
                if (lowered.contains(term.toLowerCase(Locale.ROOT))) {
                    return new NormalizedLocation(cleaned, alias.city(), alias.region(), "KR");
                }
            }
        }
        return new NormalizedLocation(cleaned, null, null, null);
    }

    public record NormalizedLocation(String raw, String city, String region, String countryCode) {
    }
}

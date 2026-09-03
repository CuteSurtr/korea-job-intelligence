package com.kji.normalize;

public record ExperienceRequirement(Integer yearsMin, Integer yearsMax, Kind kind) {

    public static ExperienceRequirement newGraduate() {
        return new ExperienceRequirement(0, null, Kind.NEW_GRADUATE);
    }

    public static ExperienceRequirement intern() {
        return new ExperienceRequirement(0, null, Kind.INTERN);
    }

    public static ExperienceRequirement unconstrained() {
        return new ExperienceRequirement(null, null, Kind.ANY);
    }

    public static ExperienceRequirement newGraduateOrExperienced() {
        return new ExperienceRequirement(0, null, Kind.NEW_GRADUATE_OR_EXPERIENCED);
    }

    public static ExperienceRequirement minimum(int years) {
        return new ExperienceRequirement(years, null, Kind.EXPERIENCED);
    }

    public static ExperienceRequirement maximum(int years) {
        return new ExperienceRequirement(null, years, Kind.EXPERIENCED);
    }

    public static ExperienceRequirement range(int min, int max) {
        return new ExperienceRequirement(min, max, Kind.EXPERIENCED);
    }

    public enum Kind {
        INTERN,
        NEW_GRADUATE,
        NEW_GRADUATE_OR_EXPERIENCED,
        ANY,
        EXPERIENCED
    }
}

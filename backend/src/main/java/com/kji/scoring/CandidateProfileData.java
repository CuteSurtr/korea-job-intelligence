package com.kji.scoring;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CandidateProfileData(Target target, Skills skills, Preferences preferences) {

    public CandidateProfileData {
        target = target == null ? new Target(List.of(), List.of(), null) : target;
        skills = skills == null ? new Skills(List.of(), List.of(), List.of()) : skills;
        preferences = preferences == null
                ? new Preferences(List.of(), List.of(), true)
                : preferences;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Target(
            @com.fasterxml.jackson.annotation.JsonProperty("seniority_buckets")
            List<String> seniorityBuckets,
            @com.fasterxml.jackson.annotation.JsonProperty("role_families")
            List<String> roleFamilies,
            @com.fasterxml.jackson.annotation.JsonProperty("max_years_experience")
            Integer maxYearsExperience
    ) {

        public Target {
            seniorityBuckets = seniorityBuckets == null ? List.of() : List.copyOf(seniorityBuckets);
            roleFamilies = roleFamilies == null ? List.of() : List.copyOf(roleFamilies);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Skills(List<String> strong, List<String> working, List<String> interest) {

        public Skills {
            strong = strong == null ? List.of() : List.copyOf(strong);
            working = working == null ? List.of() : List.copyOf(working);
            interest = interest == null ? List.of() : List.copyOf(interest);
        }

        public Set<String> all() {
            return java.util.stream.Stream.of(strong, working, interest)
                    .flatMap(List::stream)
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Preferences(
            @com.fasterxml.jackson.annotation.JsonProperty("location_countries")
            List<String> locationCountries,
            @com.fasterxml.jackson.annotation.JsonProperty("remote_policies")
            List<String> remotePolicies,
            @com.fasterxml.jackson.annotation.JsonProperty("accepts_startup")
            boolean acceptsStartup
    ) {

        public Preferences {
            locationCountries = locationCountries == null ? List.of() : List.copyOf(locationCountries);
            remotePolicies = remotePolicies == null ? List.of() : List.copyOf(remotePolicies);
        }
    }
}

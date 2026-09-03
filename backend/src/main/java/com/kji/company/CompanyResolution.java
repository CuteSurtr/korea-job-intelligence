package com.kji.company;

public record CompanyResolution(Company company, boolean created, Method method, double confidence) {

    public enum Method {
        PROVIDER_IDENTIFIER,
        EXACT_NORMALIZED_NAME,
        ALIAS,
        TRIGRAM_SIMILARITY,
        CREATED
    }
}

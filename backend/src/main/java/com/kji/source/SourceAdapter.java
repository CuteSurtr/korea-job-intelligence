package com.kji.source;

public interface SourceAdapter {

    String sourceCode();

    AdapterKind adapterKind();

    SourceFetchResult fetch(SourceQuery query);

    default boolean supportsDirectVerification() {
        return false;
    }

    default VerificationOutcome verify(String externalKey, SourceQuery query) {
        return VerificationOutcome.INCONCLUSIVE;
    }

    enum VerificationOutcome {
        PRESENT,
        ABSENT,
        ERROR,
        INCONCLUSIVE
    }
}

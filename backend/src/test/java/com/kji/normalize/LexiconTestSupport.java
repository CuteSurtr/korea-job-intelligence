package com.kji.normalize;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class LexiconTestSupport {

    private static final Lexicon LEXICON = new Lexicon(new ObjectMapper());

    private LexiconTestSupport() {
    }

    public static Lexicon lexicon() {
        return LEXICON;
    }
}

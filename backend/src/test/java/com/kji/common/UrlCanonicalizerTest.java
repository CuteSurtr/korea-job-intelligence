package com.kji.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UrlCanonicalizerTest {

    @Test
    @DisplayName("an aggregator's tracking parameter does not change the canonical form")
    void stripsAggregatorTrackingParameter() {
        String fromAggregator =
                "https://www.coupang.jobs/en/jobs/?gh_jid=8168878&utm_source=freehire.me";
        String fromEmployer = "https://www.coupang.jobs/en/jobs/?gh_jid=8168878";

        assertThat(UrlCanonicalizer.canonicalize(fromAggregator))
                .isEqualTo(UrlCanonicalizer.canonicalize(fromEmployer));
        assertThat(UrlCanonicalizer.canonicalize(fromAggregator).orElseThrow())
                .contains("gh_jid=8168878")
                .doesNotContain("utm_source");
    }

    @Test
    @DisplayName("the identifying query parameter survives canonicalization")
    void keepsIdentifyingQueryParameter() {
        assertThat(UrlCanonicalizer.canonicalize("https://www.coupang.jobs/en/jobs/?gh_jid=8168878"))
                .hasValue("https://coupang.jobs/en/jobs?gh_jid=8168878");
    }

    @Test
    @DisplayName("host case, www, default port and trailing slash do not distinguish two URLs")
    void normalizesHostAndPath() {
        assertThat(UrlCanonicalizer.canonicalize("HTTPS://WWW.Example.COM:443/jobs/123/"))
                .hasValue("https://example.com/jobs/123");
    }

    @Test
    @DisplayName("two Ashby URLs differing only by utm collapse to one key")
    void collapsesAshbyPostingUrls() {
        String withTracking = "https://jobs.ashbyhq.com/vessl-ai/8673f35a-c56f-4250-92be-d2fd1eb6e48f"
                + "?utm_source=freehire.me";
        String withoutTracking =
                "https://jobs.ashbyhq.com/vessl-ai/8673f35a-c56f-4250-92be-d2fd1eb6e48f";

        assertThat(UrlCanonicalizer.canonicalKey(withTracking))
                .isEqualTo(UrlCanonicalizer.canonicalKey(withoutTracking));
    }

    @Test
    @DisplayName("a root path written as an empty string or a slash is the same resource")
    void treatsEmptyAndSlashRootAsOne() {
        assertThat(UrlCanonicalizer.canonicalize("https://about.daangn.com?gh_jid=7771433003"))
                .isEqualTo(UrlCanonicalizer.canonicalize("https://about.daangn.com/?gh_jid=7771433003"))
                .hasValue("https://about.daangn.com?gh_jid=7771433003");
    }

    @Test
    @DisplayName("query parameter order does not change the canonical form")
    void sortsQueryParameters() {
        assertThat(UrlCanonicalizer.canonicalize("https://example.com/j?b=2&a=1"))
                .isEqualTo(UrlCanonicalizer.canonicalize("https://example.com/j?a=1&b=2"));
    }

    @Test
    @DisplayName("a non-http scheme or an unparseable string yields no canonical form")
    void rejectsUnusableInput() {
        assertThat(UrlCanonicalizer.canonicalize("mailto:careers@example.com")).isEmpty();
        assertThat(UrlCanonicalizer.canonicalize("not a url")).isEmpty();
        assertThat(UrlCanonicalizer.canonicalize(null)).isEmpty();
        assertThat(UrlCanonicalizer.canonicalize("   ")).isEmpty();
    }
}

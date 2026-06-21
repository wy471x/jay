package com.jay.release;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class ReleaseTest {

    // ── ReleaseChannel ─────────────────────────────────────────────

    @Nested
    class ReleaseChannelTests {

        @Test
        void fromBetaFlagMapsBooleans() {
            assertEquals(ReleaseChannel.STABLE, ReleaseChannel.fromBetaFlag(false));
            assertEquals(ReleaseChannel.BETA, ReleaseChannel.fromBetaFlag(true));
        }

        @Test
        void labelMatchesChannelNames() {
            assertEquals("stable", ReleaseChannel.STABLE.label());
            assertEquals("beta", ReleaseChannel.BETA.label());
        }
    }

    // ── isBetaTag ──────────────────────────────────────────────────

    @Nested
    class IsBetaTagTests {

        @Test
        void detectsBetaCaseInsensitively() {
            for (String tag : new String[]{
                "beta", "BETA", "BeTa", "v1.0.0-beta.1", "v1.0.0-BETA.1",
                "v2.0.0-beta", "something-beta-something", "beta-1.0"
            }) {
                assertTrue(ReleaseChecker.isBetaTag(tag), tag + " should be beta");
            }
            for (String tag : new String[]{
                "", "bet", "alpha", "rc", "v1.0.0", "v1.0.0-alpha.1", "v1.0.0-rc.1"
            }) {
                assertFalse(ReleaseChecker.isBetaTag(tag), tag + " should not be beta");
            }
        }
    }

    // ── ReleaseVersion parsing ─────────────────────────────────────

    @Nested
    class ReleaseVersionTests {

        @Test
        void parseSimpleVersion() {
            var v = ReleaseVersion.parse("1.2.3");
            assertEquals(1, v.major());
            assertEquals(2, v.minor());
            assertEquals(3, v.patch());
            assertNull(v.preRelease());
        }

        @Test
        void parseVersionWithVPrefix() {
            var v = ReleaseVersion.parse("v2.0.0");
            assertEquals(2, v.major());
        }

        @Test
        void parseVersionWithPreRelease() {
            var v = ReleaseVersion.parse("1.0.0-beta.2");
            assertEquals(1, v.major());
            assertEquals("beta.2", v.preRelease());
            assertTrue(v.isBeta());
        }

        @Test
        void parseVersionWithBuildSuffix() {
            var v = ReleaseVersion.parse("v0.8.61 (abc123)");
            assertEquals(0, v.major());
            assertEquals(8, v.minor());
            assertEquals(61, v.patch());
        }

        @Test
        void parseVersionWithSpaceSuffix() {
            var v = ReleaseVersion.parse("v0.8.61 abc123");
            assertEquals(0, v.major());
            assertEquals(8, v.minor());
            assertEquals(61, v.patch());
        }

        @Test
        void parseVersionTrimsWhitespace() {
            var v = ReleaseVersion.parse("  1.0.0  ");
            assertEquals(1, v.major());
        }

        @Test
        void parseThrowsOnEmpty() {
            assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse(""));
            assertThrows(IllegalArgumentException.class, () -> ReleaseVersion.parse(null));
        }

        @Test
        void parseThrowsOnInvalid() {
            assertThrows(IllegalArgumentException.class,
                () -> ReleaseVersion.parse("not-a-version"));
        }
    }

    // ── ReleaseVersion comparison ──────────────────────────────────

    @Nested
    class ReleaseVersionComparisonTests {

        @Test
        void equalVersions() {
            assertEquals(0, ReleaseVersion.parse("1.0.0")
                .compareTo(ReleaseVersion.parse("1.0.0")));
        }

        @Test
        void newerMajorWins() {
            assertTrue(ReleaseVersion.parse("2.0.0")
                .compareTo(ReleaseVersion.parse("1.9.9")) > 0);
        }

        @Test
        void preReleaseBeforeRelease() {
            assertTrue(ReleaseVersion.parse("1.0.0-beta")
                .compareTo(ReleaseVersion.parse("1.0.0")) < 0);
        }

        @Test
        void toStringIncludesPreRelease() {
            assertEquals("1.0.0-beta.1",
                ReleaseVersion.parse("1.0.0-beta.1").toString());
        }
    }

    // ── stripVPrefix ───────────────────────────────────────────────

    @Nested
    class StripVPrefixTests {

        @Test
        void stripsV() {
            assertEquals("1.0.0", ReleaseVersion.stripVPrefix("v1.0.0"));
            assertEquals("1.0.0", ReleaseVersion.stripVPrefix("V1.0.0"));
        }

        @Test
        void preservesNonVPrefix() {
            assertEquals("1.0.0", ReleaseVersion.stripVPrefix("1.0.0"));
        }

        @Test
        void handlesNullAndEmpty() {
            assertEquals("", ReleaseVersion.stripVPrefix(null));
            assertEquals("", ReleaseVersion.stripVPrefix(""));
        }
    }

    // ── JSON parsing ───────────────────────────────────────────────

    @Nested
    class JsonParsingTests {

        @Test
        void latestTagFromReleaseJsonExtractsTagName() throws Exception {
            String body = "{\"tag_name\":\"v0.8.61\",\"name\":\"Release v0.8.61\"}";
            assertEquals("v0.8.61", ReleaseChecker.latestTagFromReleaseJson(body));
        }

        @Test
        void latestTagFromReleaseJsonThrowsOnMissing() {
            assertThrows(Exception.class,
                () -> ReleaseChecker.latestTagFromReleaseJson("{\"name\":\"no tag\"}"));
        }

        @Test
        void latestBetaTagSelectsFirstBetaRelease() throws Exception {
            String body = """
                [
                  {"tag_name": "v0.9.0"},
                  {"tag_name": "v0.9.0-rc.1"},
                  {"tag_name": "v0.9.0-beta.2"},
                  {"tag_name": "v0.9.0-beta.1"}
                ]""";
            assertEquals("v0.9.0-beta.2",
                ReleaseChecker.latestBetaTagFromReleaseListJson(body));
        }

        @Test
        void latestBetaTagReportsMissingBeta() {
            String body = "[{\"tag_name\":\"v0.9.0\"}]";
            var err = assertThrows(Exception.class,
                () -> ReleaseChecker.latestBetaTagFromReleaseListJson(body));
            assertTrue(err.getMessage().contains("no beta release found"),
                "unexpected error: " + err.getMessage());
        }
    }

    // ── compareReleaseVersions ─────────────────────────────────────

    @Nested
    class CompareReleaseVersionsTests {

        @Test
        void ignoresVPrefixAndBuildSha() {
            assertEquals(0, ReleaseChecker.compareReleaseVersions(
                "0.8.39 (eeccf7d)", "v0.8.39"));
            assertTrue(ReleaseChecker.compareReleaseVersions(
                "0.8.39", "v0.8.40") < 0);
            assertTrue(ReleaseChecker.compareReleaseVersions(
                "0.8.40", "v0.8.39") > 0);
        }
    }

    // ── updateIsNeeded ─────────────────────────────────────────────

    @Nested
    class UpdateIsNeededTests {

        @Test
        void stableUpdateNeededOnlyWhenLatestIsNewer() {
            assertTrue(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.STABLE, "0.8.45", "v0.8.46"));
            assertTrue(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.STABLE, "0.8.45", "v0.9.0-beta.1"));
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.STABLE, "0.8.45", "v0.8.45"));
            // Stable should NOT consider newer if latest is pre-release older than current
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.STABLE, "0.9.0", "v0.9.0-beta.1"));
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.STABLE, "0.9.0-beta.2", "v0.9.0-beta.1"));
        }

        @Test
        void betaAllowsSwitchingFromSameStableToBeta() {
            assertTrue(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.BETA, "1.0.0", "v1.0.0-beta.2"));
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.BETA, "1.0.0-beta.2", "v1.0.0-beta.2"));
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.BETA, "1.0.0-beta.3", "v1.0.0-beta.2"));
            assertTrue(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.BETA, "1.0.0-beta.2", "v1.0.0-beta.3"));
            // Beta from newer stable to older beta: reject
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.BETA, "2.0.0", "v1.0.0-beta.3"));
            // RC is not beta: reject
            assertFalse(ReleaseChecker.updateIsNeeded(
                ReleaseChannel.BETA, "1.0.0-rc.1", "v1.0.0-beta.3"));
        }
    }

    // ── Mirror URL construction ────────────────────────────────────

    @Nested
    class MirrorUrlTests {

        @Test
        void cnbReleaseBaseUrlIncludesTagDirectory() {
            assertEquals(
                "https://cnb.cool/Hmbown/CodeWhale/-/releases/v0.8.47",
                ReleaseChecker.cnbReleaseBaseUrl("0.8.47"));
            assertEquals(
                "https://cnb.cool/Hmbown/CodeWhale/-/releases/v0.8.47",
                ReleaseChecker.cnbReleaseBaseUrl("v0.8.47"));
        }

        @Test
        void mirrorAssetUrlTrimsTrailingBaseSlashes() {
            for (String baseUrl : new String[]{
                "https://example.com/assets",
                "https://example.com/assets/",
                "https://example.com/assets//"
            }) {
                assertEquals("https://example.com/assets/file.zip",
                    ReleaseChecker.mirrorAssetUrl(baseUrl, "file.zip"),
                    baseUrl + " should join with a single slash");
            }
            assertEquals("/file.zip",
                ReleaseChecker.mirrorAssetUrl("", "file.zip"));
        }
    }

    // ── Resolve release query (without env override) ──────────────

    @Nested
    class ResolveReleaseQueryTests {

        @Test
        void stableDefaultsToGitHubLatest() {
            var query = ReleaseChecker.resolveReleaseQuery(ReleaseChannel.STABLE);
            assertInstanceOf(ReleaseQuery.GitHubLatest.class, query);
            assertEquals(ReleaseChecker.LATEST_RELEASE_URL,
                ((ReleaseQuery.GitHubLatest) query).url());
        }

        @Test
        void betaDefaultsToGitHubReleaseList() {
            var query = ReleaseChecker.resolveReleaseQuery(ReleaseChannel.BETA);
            assertInstanceOf(ReleaseQuery.GitHubReleaseList.class, query);
            assertEquals(ReleaseChecker.RELEASES_URL,
                ((ReleaseQuery.GitHubReleaseList) query).url());
        }
    }

    // ── Network fallback hint ──────────────────────────────────────

    @Test
    void updateNetworkFallbackHintMentionsRequiredMirrorInputs() {
        String hint = ReleaseChecker.updateNetworkFallbackHint();
        assertNotNull(hint);
        assertFalse(hint.isBlank());
        assertTrue(hint.contains(ReleaseChecker.CNB_REPO_URL),
            "hint missing CNB_REPO_URL");
        assertTrue(hint.contains(ReleaseChecker.RELEASE_BASE_URL_ENV),
            "hint missing RELEASE_BASE_URL_ENV");
        assertTrue(hint.contains(ReleaseChecker.UPDATE_VERSION_ENV),
            "hint missing UPDATE_VERSION_ENV");
        assertTrue(hint.contains(ReleaseChecker.CHECKSUM_MANIFEST_ASSET),
            "hint missing CHECKSUM_MANIFEST_ASSET");
    }
}

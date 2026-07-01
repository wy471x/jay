package com.jay.tui;

import com.jay.tui.client.ChatMessage;
import com.jay.tui.core.seam.SeamConfig;
import com.jay.tui.core.seam.SeamManager;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the append-only layered context manager (SeamManager).
 */
@DisplayName("SeamManager (Soft Seam)")
class SeamManagerTest {

    // ═══════════════════════════════════════════════════════════════════
    // SeamConfig
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SeamConfig")
    class SeamConfigTests {

        @Test
        @DisplayName("default config is enabled with 192K/384K/576K thresholds")
        void defaultConfigValues() {
            var cfg = SeamConfig.defaultConfig();
            assertTrue(cfg.enabled());
            assertEquals(192_000, cfg.l1Threshold());
            assertEquals(384_000, cfg.l2Threshold());
            assertEquals(576_000, cfg.l3Threshold());
            assertEquals(16, cfg.verbatimWindowTurns());
            assertEquals("deepseek-v4-flash", cfg.seamModel());
        }

        @Test
        @DisplayName("disabled config returns enabled=false")
        void disabledConfig() {
            var cfg = SeamConfig.disabled();
            assertFalse(cfg.enabled());
        }

        @Test
        @DisplayName("maxTokensForLevel returns correct caps")
        void maxTokensPerLevel() {
            assertEquals(3200, SeamConfig.maxTokensForLevel(1));
            assertEquals(2400, SeamConfig.maxTokensForLevel(2));
            assertEquals(1600, SeamConfig.maxTokensForLevel(3));
        }

        @Test
        @DisplayName("wordLimitForLevel returns correct limits")
        void wordLimitPerLevel() {
            assertEquals(800, SeamConfig.wordLimitForLevel(1));
            assertEquals(600, SeamConfig.wordLimitForLevel(2));
            assertEquals(400, SeamConfig.wordLimitForLevel(3));
        }

        @Test
        @DisplayName("densityLabel for each level")
        void densityLabels() {
            assertTrue(SeamConfig.densityLabel(1).contains("3,200"));
            assertTrue(SeamConfig.densityLabel(2).contains("2,400"));
            assertTrue(SeamConfig.densityLabel(3).contains("1,600"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // seamLevelForActiveInput (pure function)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("seamLevelForActiveInput")
    class SeamLevelTests {

        @Test
        @DisplayName("disabled config always returns null")
        void disabledReturnsNull() {
            assertNull(SeamManager.seamLevelForActiveInput(
                    SeamConfig.disabled(), 500_000, null));
        }

        @Test
        @DisplayName("below all thresholds returns null")
        void belowThresholds() {
            var cfg = SeamConfig.defaultConfig();
            assertNull(SeamManager.seamLevelForActiveInput(cfg, 100_000, null));
            assertNull(SeamManager.seamLevelForActiveInput(cfg, 50_000, null));
        }

        @Test
        @DisplayName("triggers L1 at exactly 192K")
        void triggersL1() {
            var cfg = SeamConfig.defaultConfig();
            assertEquals(1, SeamManager.seamLevelForActiveInput(cfg, 192_000, null));
            assertEquals(1, SeamManager.seamLevelForActiveInput(cfg, 300_000, null));
        }

        @Test
        @DisplayName("triggers L2 at 384K with L1 already existing")
        void triggersL2() {
            var cfg = SeamConfig.defaultConfig();
            assertEquals(2, SeamManager.seamLevelForActiveInput(cfg, 384_000, 1));
            assertEquals(2, SeamManager.seamLevelForActiveInput(cfg, 500_000, 1));
        }

        @Test
        @DisplayName("triggers L3 at 576K with L2 already existing")
        void triggersL3() {
            var cfg = SeamConfig.defaultConfig();
            assertEquals(3, SeamManager.seamLevelForActiveInput(cfg, 576_000, 2));
            assertEquals(3, SeamManager.seamLevelForActiveInput(cfg, 1_000_000, 2));
        }

        @Test
        @DisplayName("skips L1 if already at L1, goes to L2")
        void skipsL1WhenAlreadyDone() {
            var cfg = SeamConfig.defaultConfig();
            // Already at L1, 300K input — shouldn't re-trigger L1
            assertNull(SeamManager.seamLevelForActiveInput(cfg, 300_000, 1));
            // At 384K with L1 done — should trigger L2
            assertEquals(2, SeamManager.seamLevelForActiveInput(cfg, 384_000, 1));
        }

        @Test
        @DisplayName("returns null when all three levels consumed")
        void allLevelsConsumed() {
            var cfg = SeamConfig.defaultConfig();
            assertNull(SeamManager.seamLevelForActiveInput(cfg, 1_000_000, 3));
        }

        @Test
        @DisplayName("high input with no prior level fires L1 first")
        void firesL1First() {
            var cfg = SeamConfig.defaultConfig();
            // Even with 1M tokens, fires L1 first if no prior seams
            assertEquals(1, SeamManager.seamLevelForActiveInput(cfg, 1_000_000, null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // verbatimWindowStart
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verbatimWindowStart")
    class VerbatimWindowTests {

        private SeamManager manager;

        @BeforeEach
        void setUp() {
            manager = new SeamManager(null, SeamConfig.defaultConfig(), null);
        }

        @Test
        @DisplayName("zero message count returns 0")
        void zeroCountReturnsZero() {
            assertEquals(0, manager.verbatimWindowStart(0));
        }

        @Test
        @DisplayName("small message count: verbatim window covers most messages")
        void smallCountCalculation() {
            // 5 msgs → 2 turns → min(16,2)=2 verbatim turns → 4 verbatim msgs → start=1
            assertEquals(1, manager.verbatimWindowStart(5));
            // 10 msgs → 5 turns → min(16,5)=5 → 10 verbatim msgs → start=0
            assertEquals(0, manager.verbatimWindowStart(10));
        }

        @Test
        @DisplayName("returns start index for 16-turn verbatim window")
        void sixteenTurnWindow() {
            // 16 turns = 32 messages in verbatim window
            assertEquals(18, manager.verbatimWindowStart(50)); // 50 - 32 = 18
        }

        @Test
        @DisplayName("larger message count")
        void largeCount() {
            int start = manager.verbatimWindowStart(200);
            assertEquals(200 - 32, start); // 168
        }

        @Test
        @DisplayName("exactly verbatim window size")
        void exactlyWindowSize() {
            assertEquals(0, manager.verbatimWindowStart(32)); // 16 turns * 2
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // collectSeamTexts
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("collectSeamTexts")
    class CollectSeamTextsTests {

        @Test
        @DisplayName("extracts archived_context blocks from assistant messages")
        void extractsArchivedContext() {
            var messages = List.of(
                    ChatMessage.user("hi"),
                    ChatMessage.assistant("<archived_context level=\"1\">summary</archived_context>"),
                    ChatMessage.user("more"),
                    ChatMessage.assistant("normal response")
            );

            var texts = SeamManager.collectSeamTexts(messages);
            assertEquals(1, texts.size());
            assertTrue(texts.get(0).contains("<archived_context"));
        }

        @Test
        @DisplayName("no seams returns empty list")
        void noSeamsEmptyList() {
            var messages = List.of(
                    ChatMessage.user("hi"),
                    ChatMessage.assistant("hello")
            );
            assertTrue(SeamManager.collectSeamTexts(messages).isEmpty());
        }

        @Test
        @DisplayName("multiple seams all collected")
        void multipleSeams() {
            var messages = List.of(
                    ChatMessage.assistant("<archived_context level=\"1\">L1</archived_context>"),
                    ChatMessage.user("hi"),
                    ChatMessage.assistant("<archived_context level=\"2\">L2</archived_context>")
            );
            assertEquals(2, SeamManager.collectSeamTexts(messages).size());
        }

        @Test
        @DisplayName("skips non-assistant messages with archived_context")
        void skipsNonAssistant() {
            var messages = List.of(
                    ChatMessage.user("<archived_context>not a seam</archived_context>"),
                    ChatMessage.assistant("normal")
            );
            assertTrue(SeamManager.collectSeamTexts(messages).isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // formatArchivedContext
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("formatArchivedContext")
    class FormatArchivedContextTests {

        @Test
        @DisplayName("formats valid XML block")
        void formatsXmlBlock() {
            String block = SeamManager.formatArchivedContext(
                    1, 0, 100, 2500, "deepseek-v4-flash",
                    java.time.Instant.EPOCH, "This is a summary");

            assertTrue(block.contains("<archived_context"));
            assertTrue(block.contains("level=\"1\""));
            assertTrue(block.contains("range=\"msg 0-100\""));
            assertTrue(block.contains("tokens=\"~2500\""));
            assertTrue(block.contains("density=\"~3,200 tokens\""));
            assertTrue(block.contains("model=\"deepseek-v4-flash\""));
            assertTrue(block.contains("This is a summary"));
            assertTrue(block.contains("</archived_context>"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // SeamManager construction & state
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SeamManager construction")
    class ConstructionTests {

        @Test
        @DisplayName("initial seam count is zero")
        void initialSeamCount() {
            var mgr = new SeamManager(null, SeamConfig.defaultConfig(), null);
            assertEquals(0, mgr.seamCount());
            assertNull(mgr.highestLevel());
        }

        @Test
        @DisplayName("config is accessible")
        void configAccessible() {
            var cfg = SeamConfig.defaultConfig();
            var mgr = new SeamManager(null, cfg, null);
            assertSame(cfg, mgr.config());
        }

        @Test
        @DisplayName("seam level delegates to seamLevelForActiveInput")
        void seamLevelDelegates() {
            var cfg = SeamConfig.defaultConfig();
            var mgr = new SeamManager(null, cfg, null);
            assertNull(mgr.seamLevelFor(100_000));
            assertEquals(1, mgr.seamLevelFor(200_000));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // produceSoftSeam (without LLM client)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("produceSoftSeam (no LLM client)")
    class ProduceSoftSeamTests {

        @Test
        @DisplayName("empty messages returns empty string")
        void emptyMessagesReturnsEmpty() throws Exception {
            var mgr = new SeamManager(null, SeamConfig.defaultConfig(), null);
            assertEquals("", mgr.produceSoftSeam(List.of(), 1, 0, 0));
        }

        @Test
        @DisplayName("startIdx >= endIdx returns empty")
        void invalidRangeReturnsEmpty() throws Exception {
            var mgr = new SeamManager(null, SeamConfig.defaultConfig(), null);
            var msgs = List.of(ChatMessage.user("hi"));
            assertEquals("", mgr.produceSoftSeam(msgs, 1, 1, 1));
            assertEquals("", mgr.produceSoftSeam(msgs, 1, 2, 1));
        }

        @Test
        @DisplayName("all pinned messages returns empty (nothing to summarize)")
        void allPinnedReturnsEmpty() throws Exception {
            var mgr = new SeamManager(null, SeamConfig.defaultConfig(), null);
            // Only 3 messages — all pinned by KEEP_RECENT=4
            var msgs = new ArrayList<ChatMessage>();
            for (int i = 0; i < 3; i++) {
                msgs.add(ChatMessage.user("msg" + i));
            }
            // range[0..3] → all 3 are pinned → nothing to summarize
            assertEquals("", mgr.produceSoftSeam(msgs, 1, 0, 3));
        }

        @Test
        @DisplayName("with messages to summarize calls LLM and records seam")
        void recordsSeamAfterProduction() throws Exception {
            var mgr = new SeamManager(null, SeamConfig.defaultConfig(), null);
            // 10 messages — only 4 pinned (KEEP_RECENT), 6 to summarize
            var msgs = new ArrayList<ChatMessage>();
            for (int i = 0; i < 10; i++) {
                msgs.add(ChatMessage.user("This is message number " + i
                        + " with enough content to summarize"));
            }
            String result = mgr.produceSoftSeam(msgs, 1, 0, 10);

            // Without Flash client, returns placeholder
            assertFalse(result.isEmpty(), "Should produce placeholder summary");
            assertTrue(result.contains("<archived_context"),
                    "Should contain XML block: " + result);
            assertTrue(result.contains("Flash client not configured"),
                    "Should mention missing client");

            // Seam should be recorded
            assertEquals(1, mgr.seamCount());
            assertEquals(Integer.valueOf(1), mgr.highestLevel());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // recompact (without LLM client)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("recompact")
    class RecompactTests {

        @Test
        @DisplayName("recompact produces denser summary block")
        void recompactProducesDenserBlock() throws Exception {
            var mgr = new SeamManager(null, SeamConfig.defaultConfig(), null);
            var existingSeams = List.of(
                    "<archived_context level=\"1\">L1 summary</archived_context>"
            );
            var newMsgs = List.of(
                    ChatMessage.user("additional context"),
                    ChatMessage.assistant("more details here")
            );

            String result = mgr.recompact(existingSeams, newMsgs, 2, 0, 50);

            assertFalse(result.isEmpty(), "Should produce placeholder recompact");
            assertTrue(result.contains("<archived_context"));
            assertTrue(result.contains("level=\"2\""));
            assertEquals(1, mgr.seamCount());
        }
    }
}

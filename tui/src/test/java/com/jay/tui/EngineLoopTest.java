package com.jay.tui;

import com.jay.tui.core.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EngineLoopTest {

    @Nested
    class OpDispatchTests {

        @Test
        void sendMessageGeneratesFullTurnCycle() throws InterruptedException {
            EngineHandle handle = TuiEngine.spawn(
                    EngineConfig.builder().workspace(Path.of(".")).build());
            handle.sendOp(new TuiEngineOp.SendMessage(
                    "hi", "agent", "m", true, false, false, false, List.of()));

            Thread.sleep(200);
            List<TuiEngineEvent> events = handle.drainEvents();

            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.TurnStarted));
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.MessageDelta));
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.TurnComplete));
            handle.shutdown();
        }

        @Test
        void cancelRequestFlagsCancel() {
            EngineHandle handle = TuiEngine.spawn(
                    EngineConfig.builder().workspace(Path.of(".")).build());
            assertFalse(handle.isCancelled());
            handle.cancel();
            assertTrue(handle.isCancelled());
            handle.shutdown();
        }

        @Test
        void changeModeEmitsStatus() throws InterruptedException {
            EngineHandle handle = TuiEngine.spawn(
                    EngineConfig.builder().workspace(Path.of(".")).build());
            handle.sendOp(new TuiEngineOp.ChangeMode("yolo"));
            Thread.sleep(100);

            List<TuiEngineEvent> events = handle.drainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.StatusMessage
                    && ((TuiEngineEvent.StatusMessage) e).text().contains("yolo")));
            handle.shutdown();
        }

        @Test
        void setModelEmitsModelChanged() throws InterruptedException {
            EngineHandle handle = TuiEngine.spawn(
                    EngineConfig.builder().workspace(Path.of(".")).build());
            handle.sendOp(new TuiEngineOp.SetModel("claude-4", "agent"));
            Thread.sleep(100);

            List<TuiEngineEvent> events = handle.drainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.ModelChanged
                    && "claude-4".equals(((TuiEngineEvent.ModelChanged) e).modelName())));
            handle.shutdown();
        }

        @Test
        void editLastTurnResends() throws InterruptedException {
            EngineHandle handle = TuiEngine.spawn(
                    EngineConfig.builder().workspace(Path.of(".")).build());
            // Send one message first
            handle.sendOp(new TuiEngineOp.SendMessage(
                    "first", "agent", "m", true, false, false, false, List.of()));
            Thread.sleep(100);
            handle.drainEvents();

            // Edit it
            handle.sendOp(new TuiEngineOp.EditLastTurn("corrected"));
            Thread.sleep(100);

            List<TuiEngineEvent> events = handle.drainEvents();
            assertTrue(events.stream().anyMatch(e -> e instanceof TuiEngineEvent.StatusMessage));
            handle.shutdown();
        }

        @Test
        void shutdownTerminatesEngine() throws InterruptedException {
            EngineHandle handle = TuiEngine.spawn(
                    EngineConfig.builder().workspace(Path.of(".")).build());
            assertTrue(handle.isRunning());
            handle.shutdown();
            Thread.sleep(200);
            assertFalse(handle.isRunning());
        }
    }

    @Nested
    class SessionTests {

        @Test
        void addMessageIncrementsRevision() {
            var session = new Session("m", Path.of("."), true, false);
            long rev1 = session.messagesRevision();
            session.addMessage(com.jay.tui.client.ChatMessage.user("hello"));
            assertTrue(session.messagesRevision() > rev1);
        }

        @Test
        void replaceMessagesIncrementsRevision() {
            var session = new Session("m", Path.of("."), true, false);
            session.addMessage(com.jay.tui.client.ChatMessage.user("a"));
            long rev = session.messagesRevision();
            session.replaceMessages(List.of(
                    com.jay.tui.client.ChatMessage.system("reset")));
            assertTrue(session.messagesRevision() > rev);
            assertEquals(1, session.messages().size());
        }

        @Test
        void addUsageAccumulatesTokens() {
            var session = new Session("m", Path.of("."), true, false);
            session.addUsage(100, 50, 20L, 80L);
            assertEquals(100, session.totalUsage().inputTokens());
            assertEquals(50, session.totalUsage().outputTokens());
            assertEquals(20L, session.totalUsage().cacheCreationInputTokens());
            assertEquals(80L, session.totalUsage().cacheReadInputTokens());
        }

        @Test
        void messagesDefensiveCopy() {
            var session = new Session("m", Path.of("."), true, false);
            session.addMessage(com.jay.tui.client.ChatMessage.user("x"));
            assertThrows(UnsupportedOperationException.class,
                    () -> session.messages().add(
                            com.jay.tui.client.ChatMessage.user("y")));
        }
    }

    @Nested
    class TurnContextTests {

        @Test
        void stepsTrackedCorrectly() {
            var turn = new TurnContext(5);
            assertEquals(0, turn.step());
            assertFalse(turn.atMaxSteps());
            assertTrue(turn.nextStep());
            assertEquals(1, turn.step());
        }

        @Test
        void atMaxStepsReturnsTrueWhenExhausted() {
            var turn = new TurnContext(2);
            turn.nextStep(); // step=1
            turn.nextStep(); // step=2
            assertTrue(turn.atMaxSteps());
            assertFalse(turn.nextStep());
        }

        @Test
        void cancelFlag() {
            var turn = new TurnContext(10);
            assertFalse(turn.isCancelled());
            turn.cancel();
            assertTrue(turn.isCancelled());
            turn.resetCancel();
            assertFalse(turn.isCancelled());
        }

        @Test
        void toolCallCounting() {
            var turn = new TurnContext(10);
            assertFalse(turn.hasToolCalls());
            turn.recordToolCall();
            turn.recordToolCall();
            assertTrue(turn.hasToolCalls());
            assertEquals(2, turn.toolCallCount());
        }

        @Test
        void usageTracking() {
            var turn = new TurnContext(10);
            turn.addUsage(100L, 50L);
            assertEquals(100, turn.inputTokens());
            assertEquals(50, turn.outputTokens());
            var usage = turn.toUsage();
            assertEquals(100, usage.promptTokens());
            assertEquals(50, usage.completionTokens());
        }
    }
}

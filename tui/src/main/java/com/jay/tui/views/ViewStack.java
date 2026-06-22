package com.jay.tui.views;

import dev.tamboui.terminal.Frame;
import dev.tamboui.layout.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a stack of modal overlays. The topmost modal receives input
 * and is rendered last (on top). All modals in the stack render in order.
 *
 * <p>Equivalent to Rust's ViewStack.
 */
public class ViewStack {

    private final List<ModalView> stack = new ArrayList<>();

    /** Push a modal onto the stack. It becomes the active (focused) modal. */
    public void push(ModalView view) {
        stack.add(view);
    }

    /** Remove the topmost modal. */
    public void pop() {
        if (!stack.isEmpty()) {
            stack.remove(stack.size() - 1);
        }
    }

    /** Remove all modals. */
    public void clear() {
        stack.clear();
    }

    /** Get the topmost modal, or null if stack is empty. */
    public ModalView top() {
        return stack.isEmpty() ? null : stack.get(stack.size() - 1);
    }

    /** Whether the stack has any modals. */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /** Whether composer input should be blocked (any modal with blocksComposer). */
    public boolean blocksComposer() {
        return !stack.isEmpty() && stack.get(stack.size() - 1).blocksComposer();
    }

    /** Handle a key event — dispatched to the topmost modal only. */
    public ViewAction handleKey(char character, int keyCode, boolean ctrl, boolean alt) {
        var top = top();
        if (top == null) return new ViewAction.None();
        return top.handleKey(character, keyCode, ctrl, alt);
    }

    /** Render all modals in stack order (bottom first, top last). */
    public void render(Frame frame, Rect area) {
        for (var view : stack) {
            view.render(frame, area);
        }
    }

    /** Call tick() on all modals. */
    public void tick() {
        for (var view : stack) {
            view.tick();
        }
    }
}

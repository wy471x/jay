package com.jay.ide;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Tool window factory for the Jay IDE plugin.
 * Registers a side panel in JetBrains IDEs for agent interaction.
 */
public class JayToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        var contentFactory = ContentFactory.getInstance();
        var panel = new JPanel();
        panel.add(new JLabel("Jay Coding Agent"));
        var content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}

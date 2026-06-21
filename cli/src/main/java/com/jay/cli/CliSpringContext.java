package com.jay.cli;

import org.springframework.context.ConfigurableApplicationContext;

/** Static holder for the Spring ApplicationContext started by the CLI. */
public final class CliSpringContext {

    private static volatile ConfigurableApplicationContext context;

    private CliSpringContext() {}

    public static void set(ConfigurableApplicationContext ctx) {
        context = ctx;
    }

    public static ConfigurableApplicationContext get() {
        return context;
    }

    public static <T> T getBean(Class<T> type) {
        if (context == null) {
            throw new IllegalStateException("Spring context not initialized");
        }
        return context.getBean(type);
    }

    public static <T> T getBeanOrNull(Class<T> type) {
        if (context == null) return null;
        try {
            return context.getBean(type);
        } catch (Exception e) {
            return null;
        }
    }
}

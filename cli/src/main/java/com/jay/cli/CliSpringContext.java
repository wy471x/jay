package com.jay.cli;

import org.springframework.context.ConfigurableApplicationContext;

/** Static holder for the Spring ApplicationContext started by the CLI. */
public final class CliSpringContext {

    private static volatile ConfigurableApplicationContext CONTEXT;

    private CliSpringContext() { }

    public static void set(ConfigurableApplicationContext ctx) {
        CONTEXT = ctx;
    }

    public static ConfigurableApplicationContext get() {
        return CONTEXT;
    }

    public static <T> T getBean(Class<T> type) {
        if (CONTEXT == null) {
            throw new IllegalStateException("Spring CONTEXT not initialized");
        }
        return CONTEXT.getBean(type);
    }

    public static <T> T getBeanOrNull(Class<T> type) {
        if (CONTEXT == null) return null;
        try {
            return CONTEXT.getBean(type);
        } catch (Exception e) {
            return null;
        }
    }
}

package com.jay.core;

import com.jay.agent.ModelRegistry;
import com.jay.tools.ToolRegistry;
import com.jay.execpolicy.PolicyEngine;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

/**
 * Core agent runtime — ties together all subsystems using Spring Boot.
 * This is the central configuration class for the agent engine.
 */
@SpringBootApplication(scanBasePackages = "com.jay")
public class AgentRuntime {

    @Bean
    public ThreadManager threadManager() {
        return new ThreadManager();
    }

    @Bean
    public JobManager jobManager(ThreadManager threadManager) {
        return new JobManager(threadManager);
    }

    @Bean
    public ModelRegistry modelRegistry() {
        return ModelRegistry.defaultRegistry();
    }

    @Bean
    public ToolRegistry toolRegistry() {
        return new ToolRegistry();
    }

    @Bean
    public PolicyEngine policyEngine() {
        return new PolicyEngine();
    }

    @Bean
    public HookDispatcher hookDispatcher(
            org.springframework.context.ApplicationEventPublisher publisher) {
        return new HookDispatcher(publisher);
    }

    @EventListener
    public void onHookEvent(HookEvent event) {
        // Central hook logging/monitoring point
        System.out.printf("[HOOK] %s | session=%s | %s%n",
                event.type(), event.sessionId(), event.payload());
    }
}

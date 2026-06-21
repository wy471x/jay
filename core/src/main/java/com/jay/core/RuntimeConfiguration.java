package com.jay.core;

import com.jay.agent.ModelRegistry;
import com.jay.config.model.ConfigToml;
import com.jay.core.job.JobManager;
import com.jay.core.thread.ThreadManager;
import com.jay.execpolicy.ExecPolicyEngine;
import com.jay.hooks.HookDispatcher;
import com.jay.hooks.HookSink;
import com.jay.mcp.manager.McpManager;
import com.jay.state.store.StateStore;
import com.jay.tools.ToolRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Spring configuration that wires all 7 subsystems into the Runtime.
 * Equivalent to Rust's Runtime::new() wiring.
 */
@SpringBootApplication(scanBasePackages = "com.jay")
public class RuntimeConfiguration {

    @Bean
    public ThreadManager threadManager(StateStore stateStore) {
        return new ThreadManager(stateStore, "1.0.0");
    }

    @Bean
    public JobManager jobManager(StateStore stateStore) {
        return new JobManager(stateStore);
    }

    @Bean
    public HookDispatcher hookDispatcher(List<HookSink> sinks) {
        var dispatcher = new HookDispatcher();
        for (var sink : sinks) dispatcher.addSink(sink);
        return dispatcher;
    }

    @Bean
    public Runtime runtime(ConfigToml config, ModelRegistry models,
                           ThreadManager threads, ToolRegistry tools,
                           McpManager mcp, ExecPolicyEngine policy,
                           HookDispatcher hooks, JobManager jobs) {
        return new Runtime(config, models, threads, tools, mcp, policy, hooks, jobs);
    }
}

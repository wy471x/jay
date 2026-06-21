package com.jay.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jay.cli.metrics.MetricsAggregator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Instant;
import java.util.concurrent.Callable;

/** Print usage rollup from audit log and session store. */
@Command(name = "metrics", description = "Print usage rollup from audit log and session store")
public class MetricsCommand implements Callable<Integer> {

    @Option(names = {"--json"}, description = "Emit machine-readable JSON")
    boolean json;

    @Option(names = {"--since"}, description = "Restrict to events newer than duration (e.g. 7d, 24h, 2h30m)")
    String since;

    @Override
    public Integer call() {
        try {
            Instant cutoff = MetricsAggregator.parseSince(since);
            MetricsAggregator aggregator = new MetricsAggregator();
            MetricsAggregator.Rollup rollup = aggregator.aggregate(cutoff);

            if (json) {
                ObjectMapper mapper = new ObjectMapper();
                var map = rollup.toMap();
                map.put("since", since != null ? since : "all");
                System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(map));
            } else {
                String header = since != null ? " (since " + since + ")" : " (all time)";
                System.out.println("Usage Metrics" + header);
                System.out.println("=".repeat(40));
                System.out.println(rollup.toString());
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error aggregating metrics: " + e.getMessage());
            return 1;
        }
    }
}

package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")

@JsonSubTypes({

    @JsonSubTypes.Type(value = FleetAlertChannel.Slack.class, name = "slack"),

    @JsonSubTypes.Type(value = FleetAlertChannel.Webhook.class, name = "webhook"),

    @JsonSubTypes.Type(value = FleetAlertChannel.PagerDuty.class, name = "pager_duty"),

})

public sealed interface FleetAlertChannel {

    record Slack(FleetAlertEndpoint webhook) implements FleetAlertChannel { }

    record Webhook(FleetAlertEndpoint endpoint) implements FleetAlertChannel { }

    record PagerDuty(@JsonProperty("routing_key") String routingKey, String severity) implements FleetAlertChannel { }

}

package com.jay.protocol.fleet;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public record FleetAlertEndpoint(
        @JsonAlias({"webhook_url", "endpoint_url", "url"}) String url,
        @JsonProperty("url_ref") @JsonAlias({"webhook_url_ref", "webhook_ref", "url_secret_ref"}) FleetSecretRef urlRef,
        @JsonProperty("secret_ref") @JsonAlias({"secret", "webhook_secret", "signing_secret"}) FleetSecretRef secretRef) {}

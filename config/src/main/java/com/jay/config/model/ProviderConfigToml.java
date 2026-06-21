package com.jay.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Per-provider TOML config block. Equivalent to Rust's ProviderConfigToml.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderConfigToml {
    @JsonProperty("api_key") private String apiKey;
    @JsonProperty("base_url") private String baseUrl;
    private String model;
    private String mode;
    @JsonProperty("auth_mode") private String authMode;
    @JsonProperty("insecure_skip_tls_verify") private Boolean insecureSkipTlsVerify;
    @JsonProperty("http_headers") private Map<String, String> httpHeaders = new LinkedHashMap<>();
    @JsonProperty("path_suffix") private String pathSuffix;

    public String apiKey() { return apiKey; }
    public void apiKey(String v) { apiKey = v; }
    public String baseUrl() { return baseUrl; }
    public void baseUrl(String v) { baseUrl = v; }
    public String model() { return model; }
    public void model(String v) { model = v; }
    public String mode() { return mode; }
    public void mode(String v) { mode = v; }
    public String authMode() { return authMode; }
    public void authMode(String v) { authMode = v; }
    public Boolean insecureSkipTlsVerify() { return insecureSkipTlsVerify; }
    public void insecureSkipTlsVerify(Boolean v) { insecureSkipTlsVerify = v; }
    public Map<String, String> httpHeaders() { return httpHeaders; }
    public void httpHeaders(Map<String, String> v) { httpHeaders = v; }
    public String pathSuffix() { return pathSuffix; }
    public void pathSuffix(String v) { pathSuffix = v; }
}

package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = AppRequest.Capabilities.class, name = "capabilities"),
    @JsonSubTypes.Type(value = AppRequest.ConfigGet.class, name = "config_get"),
    @JsonSubTypes.Type(value = AppRequest.ConfigSet.class, name = "config_set"),
    @JsonSubTypes.Type(value = AppRequest.ConfigUnset.class, name = "config_unset"),
    @JsonSubTypes.Type(value = AppRequest.ConfigList.class, name = "config_list"),
    @JsonSubTypes.Type(value = AppRequest.Models.class, name = "models"),
    @JsonSubTypes.Type(value = AppRequest.ThreadLoadedList.class, name = "thread_loaded_list"),
})
public sealed interface AppRequest {
    record Capabilities() implements AppRequest {}
    record ConfigGet(String key) implements AppRequest {}
    record ConfigSet(String key, String value) implements AppRequest {}
    record ConfigUnset(String key) implements AppRequest {}
    record ConfigList() implements AppRequest {}
    record Models() implements AppRequest {}
    record ThreadLoadedList() implements AppRequest {}
}

// ---- AppResponse ----

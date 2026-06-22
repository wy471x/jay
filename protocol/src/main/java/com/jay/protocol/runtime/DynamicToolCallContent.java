package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonSubTypes;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")

@JsonSubTypes({

    @JsonSubTypes.Type(value = DynamicToolCallContent.InputText.class, name = "input_text"),

    @JsonSubTypes.Type(value = DynamicToolCallContent.InputImage.class, name = "input_image"),

})

public sealed interface DynamicToolCallContent {

    record InputText(String text) implements DynamicToolCallContent { }

    record InputImage(@JsonProperty("image_url") String imageUrl) implements DynamicToolCallContent { }

}

// ---- TurnEnvironmentParams ----

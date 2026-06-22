package com.jay.protocol.runtime;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

public record DynamicToolCallResult(boolean success,

        @JsonInclude(NON_EMPTY) List<DynamicToolCallContent> content) {

    public DynamicToolCallResult {

        if (content == null) content = List.of();

    }

}

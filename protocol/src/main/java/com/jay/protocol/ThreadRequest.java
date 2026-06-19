package com.jay.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ThreadRequest.Create.class, name = "create"),
    @JsonSubTypes.Type(value = ThreadRequest.Start.class, name = "start"),
    @JsonSubTypes.Type(value = ThreadRequest.Resume.class, name = "resume"),
    @JsonSubTypes.Type(value = ThreadRequest.Fork.class, name = "fork"),
    @JsonSubTypes.Type(value = ThreadRequest.List.class, name = "list"),
    @JsonSubTypes.Type(value = ThreadRequest.Read.class, name = "read"),
    @JsonSubTypes.Type(value = ThreadRequest.SetName.class, name = "set_name"),
    @JsonSubTypes.Type(value = ThreadRequest.GoalSet.class, name = "goal_set"),
    @JsonSubTypes.Type(value = ThreadRequest.GoalGet.class, name = "goal_get"),
    @JsonSubTypes.Type(value = ThreadRequest.GoalClear.class, name = "goal_clear"),
    @JsonSubTypes.Type(value = ThreadRequest.GoalRecordProgress.class, name = "goal_record_progress"),
    @JsonSubTypes.Type(value = ThreadRequest.Archive.class, name = "archive"),
    @JsonSubTypes.Type(value = ThreadRequest.Unarchive.class, name = "unarchive"),
    @JsonSubTypes.Type(value = ThreadRequest.Message.class, name = "message"),
})
public sealed interface ThreadRequest {
    record Create(JsonNode metadata) implements ThreadRequest {}
    record Start(ThreadStartParams params) implements ThreadRequest {}
    record Resume(ThreadResumeParams params) implements ThreadRequest {}
    record Fork(ThreadForkParams params) implements ThreadRequest {}
    record List(ThreadListParams params) implements ThreadRequest {}
    record Read(ThreadReadParams params) implements ThreadRequest {}
    record SetName(ThreadSetNameParams params) implements ThreadRequest {}
    record GoalSet(ThreadGoalSetParams params) implements ThreadRequest {}
    record GoalGet(ThreadGoalGetParams params) implements ThreadRequest {}
    record GoalClear(ThreadGoalClearParams params) implements ThreadRequest {}
    record GoalRecordProgress(ThreadGoalProgressParams params) implements ThreadRequest {}
    record Archive(@JsonProperty("thread_id") String threadId) implements ThreadRequest {}
    record Unarchive(@JsonProperty("thread_id") String threadId) implements ThreadRequest {}
    record Message(@JsonProperty("thread_id") String threadId, String input) implements ThreadRequest {}
}

// ---- ThreadResponse ----

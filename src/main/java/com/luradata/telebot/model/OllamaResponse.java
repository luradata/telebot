package com.luradata.telebot.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class OllamaResponse {
    @JsonProperty("model")
    private String model;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("response")
    private String response;

    @JsonProperty("done")
    private boolean done;

    @JsonProperty("context")
    private long[] context;

    @JsonProperty("total_duration")
    private long totalDuration;

    @JsonProperty("load_duration")
    private long loadDuration;

    @JsonProperty("prompt_eval_duration")
    private long promptEvalDuration;

    @JsonProperty("eval_duration")
    private long evalDuration;

    @JsonProperty("eval_count")
    private int evalCount;
} 
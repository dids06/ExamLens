package com.examlens.ai;

import java.util.List;

/** Provider-independent AI request. All prompts and schemas are produced by PromptLibrary. */
public class AiRequest {

    private final AiAction action;
    private final String systemPrompt;
    private final String userPrompt;
    private final String jsonSchema;
    private final double temperature;
    private final int maxOutputTokens;
    private final List<ImagePart> images;
    private final String correctionInstruction;
    private final String inputJson;

    private AiRequest(Builder b) {
        this.action = b.action;
        this.systemPrompt = b.systemPrompt;
        this.userPrompt = b.userPrompt;
        this.jsonSchema = b.jsonSchema;
        this.temperature = b.temperature;
        this.maxOutputTokens = b.maxOutputTokens;
        this.images = b.images == null ? List.of() : List.copyOf(b.images);
        this.correctionInstruction = b.correctionInstruction;
        this.inputJson = b.inputJson;
    }

    public AiAction getAction() { return action; }
    public String getSystemPrompt() { return systemPrompt; }
    public String getUserPrompt() { return userPrompt; }
    public String getJsonSchema() { return jsonSchema; }
    public double getTemperature() { return temperature; }
    public int getMaxOutputTokens() { return maxOutputTokens; }
    public List<ImagePart> getImages() { return images; }
    public String getCorrectionInstruction() { return correctionInstruction; }
    public String getInputJson() { return inputJson; }

    public static Builder builder(AiAction action) {
        return new Builder(action);
    }

    public Builder toBuilder() {
        return new Builder(action)
                .systemPrompt(systemPrompt)
                .userPrompt(userPrompt)
                .jsonSchema(jsonSchema)
                .temperature(temperature)
                .maxOutputTokens(maxOutputTokens)
                .images(images)
                .correctionInstruction(correctionInstruction)
                .inputJson(inputJson);
    }

    /** Base64 image payload for multimodal document understanding. */
    public record ImagePart(String mimeType, String base64Data) {
    }

    public static final class Builder {
        private final AiAction action;
        private String systemPrompt = "";
        private String userPrompt = "";
        private String jsonSchema;
        private double temperature = 0.3;
        private int maxOutputTokens = 8192;
        private List<ImagePart> images = List.of();
        private String correctionInstruction;
        private String inputJson;

        private Builder(AiAction action) {
            this.action = action;
        }

        public Builder systemPrompt(String v) { this.systemPrompt = v; return this; }
        public Builder userPrompt(String v) { this.userPrompt = v; return this; }
        public Builder jsonSchema(String v) { this.jsonSchema = v; return this; }
        public Builder temperature(double v) { this.temperature = v; return this; }
        public Builder maxOutputTokens(int v) { this.maxOutputTokens = v; return this; }
        public Builder images(List<ImagePart> v) { this.images = v; return this; }
        public Builder correctionInstruction(String v) { this.correctionInstruction = v; return this; }
        public Builder inputJson(String v) { this.inputJson = v; return this; }

        public AiRequest build() {
            return new AiRequest(this);
        }
    }
}

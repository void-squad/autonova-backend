package com.voidsquad.chatbot.service.language.provider;

public class ChatResponse {
    private final String text;

    public ChatResponse(String text) {
        this.text = text;
    }

    public Result getResult() {
        return new Result(text);
    }

    public static class Result {
        private final Output output;

        public Result(String text) {
            this.output = new Output(text);
        }

        public Output getOutput() {
            return output;
        }
    }

    public static class Output {
        private final String text;

        public Output(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}

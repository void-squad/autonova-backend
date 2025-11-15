package com.voidsquad.chatbot.service.tool;

/**
 * Execution result returned by a Tool.
 */
public class ToolCallResult {
    private final boolean success;
    private final String result;
    private final String message;
    private String toolName;

    public ToolCallResult(boolean success, String result, String message, String toolName) {
        this.success = success;
        this.result = result;
        this.message = message;
        this.toolName = toolName;
    }

    public boolean isSuccess() { return success; }
    public Object getResult() { return result; }
    public String getMessage() { return message; }
    public String getToolName() { return toolName; }

    public static ToolCallResult success(String result,String toolName) { return new ToolCallResult(true, result, null,toolName); }
    public static ToolCallResult failure(String message,String toolName) { return new ToolCallResult(false, null, message,toolName); }
}

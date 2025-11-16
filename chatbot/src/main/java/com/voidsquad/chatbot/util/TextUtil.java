package com.voidsquad.chatbot.util;

/**
 * Small text helpers used by prompt post-processing.
 */
public final class TextUtil {
    private TextUtil() {}

    /**
     * Strip common code-fence wrappers such as ```json ... ``` or '''json ... ''' from the given text.
     * If no fence is found the original string (trimmed) is returned.
     */
    public static String stripCodeFences(String raw) {
        if (raw == null) return null;
        String t = raw.trim();

        // backtick fences: ``` or ```lang
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return t.substring(firstNewline + 1, lastFence).trim();
            }
            if (t.startsWith("```") && t.endsWith("```")) {
                return t.substring(3, t.length() - 3).trim();
            }
            // otherwise fall through — we'll return the trimmed original at end
        }

        // single-quote fences: ''' or '''lang
        if (t.startsWith("'''")) {
            int firstNewline = t.indexOf('\n');
            int lastFence = t.lastIndexOf("'''");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return t.substring(firstNewline + 1, lastFence).trim();
            }
            if (t.startsWith("'''") && t.endsWith("'''")) {
                return t.substring(3, t.length() - 3).trim();
            }
        }

        return t;
    }
}

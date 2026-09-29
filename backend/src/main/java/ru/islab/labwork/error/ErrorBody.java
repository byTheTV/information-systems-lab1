package ru.islab.labwork.error;

import java.util.Map;

public class ErrorBody {
    public String message;
    public Map<String, String> fields;
    public Boolean requiresReplacement;

    public static ErrorBody message(String message) {
        ErrorBody body = new ErrorBody();
        body.message = message;
        return body;
    }

    public static ErrorBody of(String message, Map<String, String> fields) {
        ErrorBody body = message(message);
        body.fields = fields;
        return body;
    }
}

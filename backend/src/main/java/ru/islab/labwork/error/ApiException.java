package ru.islab.labwork.error;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

public class ApiException extends WebApplicationException {

    public ApiException(Response response) {
        super(response);
    }

    public static ApiException of(int status, String message) {
        return new ApiException(json(status, ErrorBody.message(message)));
    }

    public static ApiException fields(String message, Map<String, String> fields) {
        return new ApiException(json(400, ErrorBody.of(message, fields)));
    }

    public static ApiException replacement(String message) {
        ErrorBody body = ErrorBody.message(message);
        body.requiresReplacement = true;
        return new ApiException(json(409, body));
    }

    private static Response json(int status, ErrorBody body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}

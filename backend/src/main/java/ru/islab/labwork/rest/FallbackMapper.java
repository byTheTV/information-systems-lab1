package ru.islab.labwork.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import ru.islab.labwork.error.ErrorBody;

@Provider
@ApplicationScoped
public class FallbackMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(FallbackMapper.class.getName());

    @Override
    public Response toResponse(Exception exception) {
        if (exception instanceof WebApplicationException web) {
            Response response = web.getResponse();
            if (response != null && response.getEntity() != null) {
                return response;
            }
            int status = response == null ? 500 : response.getStatus();
            String message = web.getMessage() == null ? "Ошибка" : web.getMessage();
            return json(status, ErrorBody.message(message));
        }

        ConstraintViolationException violations = find(exception, ConstraintViolationException.class);
        if (violations != null) {
            Map<String, String> fields = new LinkedHashMap<>();
            for (ConstraintViolation<?> violation : violations.getConstraintViolations()) {
                fields.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return json(400, ErrorBody.of("Проверьте поля формы", fields));
        }

        SQLException sql = find(exception, SQLException.class);
        if (sql != null && sql.getSQLState() != null) {
            String text = clean(sql.getMessage());
            return switch (sql.getSQLState()) {
                case "P0001" -> json(400, ErrorBody.message(text));
                case "23505" -> json(409, ErrorBody.message("Такая запись уже есть"));
                case "23514", "23502", "23503" -> json(400, ErrorBody.message("Данные отклонены базой: " + text));
                default -> serverError(exception);
            };
        }

        if (badJson(exception)) {
            return json(400, ErrorBody.message("Некорректный формат запроса"));
        }
        return serverError(exception);
    }

    private static Response serverError(Exception exception) {
        LOG.log(Level.SEVERE, "request failed", exception);
        return json(500, ErrorBody.message("Внутренняя ошибка сервера"));
    }

    private static boolean badJson(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            String name = current.getClass().getName();
            if (name.contains("Json") || name.contains("jsonb")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static String clean(String message) {
        if (message == null || message.isBlank()) {
            return "Ошибка базы данных";
        }
        String line = message.split("\\R", 2)[0];
        if (line.startsWith("ERROR: ")) {
            line = line.substring("ERROR: ".length());
        }
        return line;
    }

    private static Response json(int status, ErrorBody body) {
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }

    private static <T extends Throwable> T find(Throwable root, Class<T> type) {
        ArrayDeque<Throwable> stack = new ArrayDeque<>();
        Set<Throwable> seen = new HashSet<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Throwable current = stack.pop();
            if (current == null || !seen.add(current)) {
                continue;
            }
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            if (current.getCause() != null) {
                stack.push(current.getCause());
            }
            for (Throwable suppressed : current.getSuppressed()) {
                if (suppressed != null) {
                    stack.push(suppressed);
                }
            }
        }
        return null;
    }
}

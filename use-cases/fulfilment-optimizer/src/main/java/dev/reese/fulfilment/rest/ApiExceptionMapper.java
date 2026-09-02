package dev.reese.fulfilment.rest;

import dev.reese.fulfilment.persistence.IdempotencyConflictException;
import dev.reese.fulfilment.persistence.JobNotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Override
    public Response toResponse(RuntimeException exception) {
        if (exception instanceof IdempotencyConflictException) {
            return error(Response.Status.CONFLICT, exception);
        }
        if (exception instanceof JobNotFoundException) {
            return error(Response.Status.NOT_FOUND, exception);
        }
        if (exception instanceof IllegalArgumentException) {
            return error(Response.Status.BAD_REQUEST, exception);
        }
        return error(Response.Status.INTERNAL_SERVER_ERROR, exception);
    }

    private Response error(Response.Status status, RuntimeException exception) {
        return Response.status(status)
                .entity(new ApiError(status.getStatusCode(), status.getReasonPhrase(), exception.getMessage()))
                .build();
    }
}

package com.perapoch.tasksapp.exception;

import com.perapoch.tasksapp.core.task.InvalidParameterException;
import com.perapoch.tasksapp.core.task.TaskAlreadyExistsException;
import com.perapoch.tasksapp.core.task.TaskNotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InternalExceptionMapper implements ExceptionMapper<InternalException> {

    private static final Logger logger = LoggerFactory.getLogger(InternalExceptionMapper.class);

    @Override
    public Response toResponse(InternalException exception) {
        if (exception instanceof InvalidParameterException || exception instanceof TaskAlreadyExistsException) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity(exception.getMessage())
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        } else if (exception instanceof TaskNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND)
                           .entity(exception.getMessage())
                           .type(MediaType.TEXT_PLAIN)
                           .build();
        }
        logger.error("Got InternalException", exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                       .entity("Something went wrong with your request. Please try again later.")
                       .type(MediaType.TEXT_PLAIN)
                       .build();
    }
}

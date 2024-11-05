/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import jakarta.ws.rs.core.Response;
import se.laz.casual.http.resources.CasualContentType;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ExceptionHandlerImpl implements ExceptionHandler
{
    private static final Logger LOG = Logger.getLogger(ExceptionHandlerImpl.class.getName());
    @Override
    public Response handle(Exception e)
    {
        LOG.log(Level.WARNING, e.getMessage(), e);
        return Response.serverError().header("content-type", CasualContentType.NULL).build();
    }
}

/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import jakarta.ws.rs.core.Response;
import se.laz.casual.api.flags.ErrorState;

public class ErrorStateConverter
{
    private ErrorStateConverter()
    {}
    public static Response.Status convert(ErrorState state)
    {
        switch (state)
        {
            case ErrorState.TPENOENT: return Response.Status.NOT_FOUND;
            case ErrorState.TPETIME: return Response.Status.REQUEST_TIMEOUT;
            default: return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}

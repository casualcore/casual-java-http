/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import jakarta.ejb.EJBTransactionRolledbackException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import se.laz.casual.http.resources.CasualContentType;

@Provider
public class EJBTransactionRolledbackExceptionMapper implements ExceptionMapper<EJBTransactionRolledbackException>
{
    @Override
    public Response toResponse(EJBTransactionRolledbackException e)
    {
        // note:
        // must be JTA transaction boundary timeout since all methods, local and remote should be non-transactional
        return Response.status(Response.Status.REQUEST_TIMEOUT).header(HttpHeaders.CONTENT_TYPE, CasualContentType.NULL).build();
    }
}

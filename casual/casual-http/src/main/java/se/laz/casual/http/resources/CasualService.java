/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import se.laz.casual.api.buffer.type.CStringBuffer;
import se.laz.casual.api.buffer.type.JsonBuffer;
import se.laz.casual.api.buffer.type.OctetBuffer;
import se.laz.casual.api.buffer.type.fielded.FieldedTypeBuffer;
import se.laz.casual.http.resources.handlers.ExceptionHandler;
import se.laz.casual.http.resources.handlers.RequestHandler;

import java.io.InputStream;
import java.util.Collections;

@Stateless
@Path("/casual")
public class CasualService
{
    private ServiceCaller serviceCaller;
    private RequestHandler requestHandler;
    private ExceptionHandler exceptionHandler;

    public CasualService()
    {
        // NOP ctor needed for CDI
    }

    @Inject
    public CasualService(ServiceCaller serviceCaller, RequestHandler requestHandler, ExceptionHandler exceptionHandler)
    {
        this.serviceCaller = serviceCaller;
        this.requestHandler = requestHandler;
        this.exceptionHandler = exceptionHandler;
    }

    @POST
    @Consumes(CasualContentType.X_OCTET)
    @Path("{serviceName}")
    public Response serviceRequestCasualXOctet(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        return requestHandler.handle(serviceCaller, serviceName, inputStream, OctetBuffer::of, exceptionHandler::handle);
    }

    @POST
    @Consumes(CasualContentType.JSON)
    @Path("{serviceName}")
    public Response serviceRequestJson(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        return requestHandler.handle(serviceCaller, serviceName, inputStream, data -> JsonBuffer.of(Collections.singletonList(data)), exceptionHandler::handle);
    }

    @POST
    @Consumes(CasualContentType.FIELD)
    @Path("{serviceName}")
    public Response serviceRequestField(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        return requestHandler.handle(serviceCaller, serviceName, inputStream, data -> FieldedTypeBuffer.create(Collections.singletonList(data)), exceptionHandler::handle);
    }

    @POST
    @Consumes(CasualContentType.STRING)
    @Path("{serviceName}")
    public Response serviceRequestCString(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        return requestHandler.handle(serviceCaller, serviceName, inputStream, data -> CStringBuffer.of(Collections.singletonList(data)), exceptionHandler::handle);
    }


}

/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import se.laz.casual.api.buffer.CasualBufferType;
import se.laz.casual.api.buffer.type.CStringBuffer;
import se.laz.casual.api.buffer.type.JsonBuffer;
import se.laz.casual.api.buffer.type.OctetBuffer;
import se.laz.casual.api.buffer.type.fielded.FieldedTypeBuffer;
import se.laz.casual.http.resources.handlers.CasualServiceCallWorkCreator;
import se.laz.casual.http.resources.handlers.ExceptionHandler;
import se.laz.casual.http.resources.handlers.LocalRequestHandler;
import se.laz.casual.http.resources.handlers.RemoteRequestHandler;
import se.laz.casual.jca.inflow.work.CasualServiceCallWork;

import java.io.InputStream;
import java.util.Collections;

@Stateless
@Path("/casual")
public class CasualService
{
    private ServiceCaller serviceCaller;
    private RemoteRequestHandler remoteRequestHandler;
    private LocalRequestHandler localRequestHandler;
    private ExceptionHandler exceptionHandler;
    private ServiceRegistryLookup serviceRegistryLookup;
    private CasualServiceCallWorkCreator workCreator;
    @Resource
    ManagedExecutorService executorService;
    public CasualService()
    {
        // NOP ctor needed for CDI
    }

    @Inject
    public CasualService(ServiceCaller serviceCaller, RemoteRequestHandler remoteRequestHandler, LocalRequestHandler localRequestHandler, ExceptionHandler exceptionHandler, ServiceRegistryLookup serviceRegistryLookup)
    {
        this.serviceCaller = serviceCaller;
        this.remoteRequestHandler = remoteRequestHandler;
        this.localRequestHandler = localRequestHandler;
        this.exceptionHandler = exceptionHandler;
        this.serviceRegistryLookup = serviceRegistryLookup;
        this.workCreator = CasualServiceCallWork::new;
    }

    @POST
    @Consumes(CasualContentType.X_OCTET)
    @Path("{serviceName}")
    public Response serviceRequestCasualXOctet(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        if(serviceRegistryLookup.serviceExists(serviceName))
        {
            return localRequestHandler.handle(serviceName, inputStream, CasualBufferType.X_OCTET, workCreator, exceptionHandler::handle);
        }
        return remoteRequestHandler.handle(serviceCaller, serviceName, inputStream, OctetBuffer::of, exceptionHandler::handle);
    }

    @POST
    @Consumes(CasualContentType.JSON)
    @Path("{serviceName}")
    public Response serviceRequestJson(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        if(serviceRegistryLookup.serviceExists(serviceName))
        {
            return localRequestHandler.handle(serviceName, inputStream, CasualBufferType.JSON, workCreator, exceptionHandler::handle);
        }
        return remoteRequestHandler.handle(serviceCaller, serviceName, inputStream, data -> JsonBuffer.of(Collections.singletonList(data)), exceptionHandler::handle);
    }

    @POST
    @Consumes(CasualContentType.FIELD)
    @Path("{serviceName}")
    public Response serviceRequestField(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        if(serviceRegistryLookup.serviceExists(serviceName))
        {
            return localRequestHandler.handle(serviceName, inputStream, CasualBufferType.FIELDED, workCreator, exceptionHandler::handle);
        }
        return remoteRequestHandler.handle(serviceCaller, serviceName, inputStream, data -> FieldedTypeBuffer.create(Collections.singletonList(data)), exceptionHandler::handle);
    }

    @POST
    @Consumes(CasualContentType.STRING)
    @Path("{serviceName}")
    public Response serviceRequestCString(@PathParam("serviceName") String serviceName, InputStream inputStream)
    {
        if(serviceRegistryLookup.serviceExists(serviceName))
        {
            return localRequestHandler.handle(serviceName, inputStream, CasualBufferType.CSTRING, workCreator, exceptionHandler::handle);
        }
        return remoteRequestHandler.handle(serviceCaller, serviceName, inputStream, data -> CStringBuffer.of(Collections.singletonList(data)), exceptionHandler::handle);
    }


}

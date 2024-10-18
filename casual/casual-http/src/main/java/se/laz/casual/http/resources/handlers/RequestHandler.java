/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;
import se.laz.casual.api.buffer.CasualBuffer;
import se.laz.casual.api.buffer.CasualBufferType;
import se.laz.casual.api.flags.AtmiFlags;
import se.laz.casual.api.flags.Flag;
import se.laz.casual.api.flags.ServiceReturnState;
import se.laz.casual.http.resources.ErrorStateConverter;
import se.laz.casual.http.resources.CasualContentType;
import se.laz.casual.http.resources.ContentTypeConverter;
import se.laz.casual.http.resources.ServiceCallResponse;
import se.laz.casual.http.resources.ServiceCaller;

import java.io.InputStream;
import java.util.logging.Logger;

public class RequestHandler
{
    private static final Logger LOG = Logger.getLogger(RequestHandler.class.getName());
    public Response handle(ServiceCaller serviceCaller, String serviceName, InputStream inputStream, CasualBufferCreator bufferCreator, ExceptionHandler exceptionHandler)
    {
        try
        {
            byte[] data = IOUtils.toByteArray(inputStream);
            Flag<AtmiFlags> flags = Flag.of(AtmiFlags.TPNOTRAN);
            CasualBuffer buffer = bufferCreator.create(data);
            ServiceCallResponse serviceCallResponse = serviceCaller.makeServiceCall(buffer, serviceName, flags);
            if(serviceCallResponse.serviceReturnState() == ServiceReturnState.TPSUCCESS)
            {
                CasualBuffer responseBuffer = serviceCallResponse.casualBuffer();
                String contentType = ContentTypeConverter.convert(CasualBufferType.unmarshall(responseBuffer.getType()));
                StreamingOutput stream = outputStream -> outputStream.write(responseBuffer.getBytes().getFirst());
                return Response.ok(stream).header("content-type", contentType).build();
            }
            Response.Status status = ErrorStateConverter.convert(serviceCallResponse.errorState());
            LOG.finest(() -> "service call to " + serviceName + " failed: " + serviceCallResponse.serviceReturnState() + " " + serviceCallResponse.errorState());
            return Response.status(status).header("content-type", CasualContentType.NULL).build();
        }
        catch (Exception e)
        {
            return exceptionHandler.handle(e);
        }
    }
}

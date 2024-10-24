/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import org.apache.commons.io.IOUtils;
import se.laz.casual.api.buffer.CasualBufferType;
import se.laz.casual.api.buffer.type.ServiceBuffer;
import se.laz.casual.api.flags.AtmiFlags;
import se.laz.casual.api.flags.ErrorState;
import se.laz.casual.api.flags.Flag;
import se.laz.casual.api.xa.XID;
import se.laz.casual.http.resources.CasualContentType;
import se.laz.casual.http.resources.ContentTypeConverter;
import se.laz.casual.jca.inflow.work.CasualServiceCallWork;
import se.laz.casual.network.protocol.messages.service.CasualServiceCallReplyMessage;
import se.laz.casual.network.protocol.messages.service.CasualServiceCallRequestMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LocalRequestHandler
{
    @Resource
    private ManagedExecutorService executorService;

    public Response handle(String serviceName, InputStream inputStream, CasualBufferType bufferType, CasualServiceCallWorkCreator workCreator, ExceptionHandler exceptionHandler)
    {
        try
        {
            List<byte[]> payload = new ArrayList<>();
            payload.add(IOUtils.toByteArray(inputStream));
            ServiceBuffer serviceBuffer = ServiceBuffer.of(bufferType.getName(), payload);
            CasualServiceCallRequestMessage requestMessage =
                    CasualServiceCallRequestMessage.createBuilder()
                                                   .setServiceBuffer(serviceBuffer)
                                                   .setServiceName(serviceName)
                                                   .setXatmiFlags(Flag.of(AtmiFlags.TPNOTRAN))
                                                   .setXid(XID.NULL_XID)
                                                   .build();
            CasualServiceCallWork work = workCreator.create(UUID.randomUUID(), requestMessage);
            executorService.runAsync(work).join();
            CasualServiceCallReplyMessage replyMessage = work.getResponse().getMessage();
            if(replyMessage.getError() == ErrorState.OK)
            {
                String contentType = ContentTypeConverter.convert(CasualBufferType.unmarshall(replyMessage.getServiceBuffer().getType()));
                StreamingOutput stream = outputStream -> outputStream.write(replyMessage.getServiceBuffer().getBytes().getFirst());
                return Response.ok(stream).header(HttpHeaders.CONTENT_TYPE, contentType).build();
            }
            return createErrorResponse(replyMessage);
        }
        catch (Exception e)
        {
            return exceptionHandler.handle(e);
        }
    }

    private Response createErrorResponse(CasualServiceCallReplyMessage replyMessage)
    {
        Response.Status status = replyMessage.getError() == ErrorState.TPETIME ? Response.Status.REQUEST_TIMEOUT : Response.Status.INTERNAL_SERVER_ERROR;
        if(null != replyMessage.getServiceBuffer() && !replyMessage.getServiceBuffer().isEmpty())
        {
            String contentType = ContentTypeConverter.convert(CasualBufferType.unmarshall(replyMessage.getServiceBuffer().getType()));
            StreamingOutput stream = outputStream -> outputStream.write(replyMessage.getServiceBuffer().getBytes().getFirst());
            return Response.status(status).entity(stream).header("content-type", contentType).build();
        }
        return Response.status(status).header(HttpHeaders.CONTENT_TYPE, CasualContentType.NULL).build();
    }
}

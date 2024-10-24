/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources

import jakarta.enterprise.concurrent.ManagedExecutorService
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import org.jboss.resteasy.mock.MockDispatcherFactory
import org.jboss.resteasy.mock.MockHttpRequest
import org.jboss.resteasy.mock.MockHttpResponse
import org.jboss.resteasy.spi.Dispatcher
import se.laz.casual.api.buffer.CasualBuffer
import se.laz.casual.api.buffer.type.CStringBuffer
import se.laz.casual.api.buffer.type.JsonBuffer
import se.laz.casual.api.buffer.type.OctetBuffer
import se.laz.casual.api.buffer.type.ServiceBuffer
import se.laz.casual.api.buffer.type.fielded.FieldedTypeBuffer
import se.laz.casual.api.flags.AtmiFlags
import se.laz.casual.api.flags.ErrorState
import se.laz.casual.api.flags.Flag
import se.laz.casual.api.flags.ServiceReturnState
import se.laz.casual.api.xa.XID
import se.laz.casual.http.resources.handlers.CasualServiceCallWorkCreator
import se.laz.casual.http.resources.handlers.ExceptionHandler
import se.laz.casual.http.resources.handlers.ExceptionHandlerImpl
import se.laz.casual.http.resources.handlers.LocalRequestHandler
import se.laz.casual.http.resources.handlers.RemoteRequestHandler
import se.laz.casual.jca.inflow.work.CasualServiceCallWork
import se.laz.casual.network.protocol.messages.CasualNWMessageImpl
import se.laz.casual.network.protocol.messages.service.CasualServiceCallReplyMessage
import se.laz.casual.network.protocol.messages.service.CasualServiceCallRequestMessage
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import java.util.stream.Collectors

class CasualServiceTest extends Specification
{
   @Shared
   def root = '/casual'
   @Shared
   def serviceName = 'echo'
   @Shared
   def content = '{"msg":"bazinga!"}'
   @Shared
   def key = 'FLD_STRING1'
   @Shared
   def serviceRegistryLookupServiceDoesNotExist = Mock(ServiceRegistryLookup) {
      serviceExists(_) >> false
   }
   @Shared
   def serviceRegistryLookupServiceExists = Mock(ServiceRegistryLookup) {
      serviceExists(_) >> true
   }

   @Unroll
   def 'remote service call ok #mimeType #expectedReturnMimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            new ServiceCallResponse(ServiceReturnState.TPSUCCESS, ErrorState.OK, replyBuffer)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), Mock(ExceptionHandler), serviceRegistryLookupServiceDoesNotExist)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == Response.Status.OK.statusCode
      response.outputHeaders[HttpHeaders.CONTENT_TYPE].first.toString().contains(expectedReturnMimeType)
      CasualBuffer responseBuffer = creatorFunction(response.output)
      responseBuffer.getBytes() == requestBuffer.getBytes()
      where:
      mimeType                  || expectedReturnMimeType    || requestBuffer                                              || replyBuffer                                                || creatorFunction
      CasualContentType.X_OCTET || CasualContentType.X_OCTET || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.FIELD   || CasualContentType.FIELD   || FieldedTypeBuffer.create().write( key, content)            || FieldedTypeBuffer.create().write( key, content)            || {bytes -> FieldedTypeBuffer.create([bytes])}
      CasualContentType.JSON    || CasualContentType.JSON    || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || {bytes -> JsonBuffer.of([bytes])}
      CasualContentType.STRING  || CasualContentType.STRING  || CStringBuffer.of(content)                                  || CStringBuffer.of(content)                                  || {bytes -> CStringBuffer.of([bytes])}
      CasualContentType.X_OCTET || CasualContentType.JSON    || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || {bytes -> JsonBuffer.of([bytes])}
      CasualContentType.JSON    || CasualContentType.X_OCTET || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.X_OCTET || CasualContentType.JSON    || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || {bytes -> JsonBuffer.of([bytes])}
   }

   @Unroll
   def 'remote service call error #errorState #mimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            new ServiceCallResponse(ServiceReturnState.TPFAIL, errorState, replyBuffer)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), Mock(ExceptionHandler), serviceRegistryLookupServiceDoesNotExist)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == ErrorStateConverter.convert(errorState).statusCode
      response.outputHeaders[HttpHeaders.CONTENT_TYPE].first == (CasualContentType.NULL)
      response.output.size() == 0
      where:
      mimeType                  || errorState          || requestBuffer                                              || replyBuffer  || creatorFunction
      CasualContentType.X_OCTET || ErrorState.TPENOENT || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || null         || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.FIELD   || ErrorState.TPETIME  || FieldedTypeBuffer.create().write( key, content)            || null         || {bytes -> FieldedTypeBuffer.create([bytes])}
   }

   @Unroll
   def 'remote service call error but with reply buffer #errorState #mimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            new ServiceCallResponse(ServiceReturnState.TPFAIL, errorState, replyBuffer)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), Mock(ExceptionHandler), serviceRegistryLookupServiceDoesNotExist)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      CasualBuffer responseBuffer = creatorFunction(response.getOutput())
      response.getStatus() == ErrorStateConverter.convert(errorState).statusCode
      response.outputHeaders[HttpHeaders.CONTENT_TYPE].first.toString().contains(mimeType)
      responseBuffer.getBytes() == replyBuffer.getBytes()
      where:
      mimeType                  || errorState           || requestBuffer                                              || replyBuffer                                                        || creatorFunction
      CasualContentType.X_OCTET || ErrorState.TPESVCERR || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)])         || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.FIELD   || ErrorState.TPESVCFAIL|| FieldedTypeBuffer.create().write( key, content)            || FieldedTypeBuffer.create().write( key, content)                    || {bytes -> FieldedTypeBuffer.create([bytes])}
   }

   @Unroll
   def 'remote serviceCall exceptional #mimeType'()
   {
      given:
      def errorMessage = 'very illegal'
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            throw new IllegalArgumentException(errorMessage)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), new ExceptionHandlerImpl(), serviceRegistryLookupServiceDoesNotExist)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.statusCode
      response.outputHeaders[HttpHeaders.CONTENT_TYPE].first.toString() == CasualContentType.NULL
      response.output.size() == 0
      where:
      mimeType                           || requestBuffer
      CasualContentType.X_OCTET          || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)])
      CasualContentType.FIELD            || FieldedTypeBuffer.create().write( key, content)
      CasualContentType.JSON             || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])
      CasualContentType.STRING           || CStringBuffer.of(content)
   }

   // local service tests
   @Unroll
   def 'local service call ok #mimeType #expectedReturnMimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      CompletableFuture<Void> future = new CompletableFuture<>()
      ManagedExecutorService executorService = Mock(ManagedExecutorService){
         runAsync(_) >> {
            future.complete(null)
            return future
         }
      }
      LocalRequestHandler localRequestHandler = new LocalRequestHandler()
      localRequestHandler.executorService = executorService
      ServiceBuffer serviceBuffer = new ServiceBuffer(replyBuffer.getType(), replyBuffer.getBytes().stream().collect(Collectors.toList()))
      CasualServiceCallReplyMessage replyMessage = CasualServiceCallReplyMessage.createBuilder()
              .setServiceBuffer(serviceBuffer)
              .setError(ErrorState.OK)
              .setXid(XID.NULL_XID)
              .build()
      CasualServiceCallWork work
      CasualServiceCallWorkCreator workCreator = Mock(CasualServiceCallWorkCreator){
         create(_, _) >> { UUID correlationId, CasualServiceCallRequestMessage requestMessage ->
            CasualServiceCallWork createdWork = new CasualServiceCallWork(correlationId, requestMessage)
            createdWork.response = CasualNWMessageImpl.of(correlationId, replyMessage)
            work = createdWork
         }
      }
      CasualService casualService = new CasualService(Mock(ServiceCaller), Mock(RemoteRequestHandler), localRequestHandler, Mock(ExceptionHandler), serviceRegistryLookupServiceExists)
      casualService.workCreator = workCreator
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == Response.Status.OK.statusCode
      response.outputHeaders[HttpHeaders.CONTENT_TYPE].first.toString().contains(expectedReturnMimeType)
      CasualBuffer responseBuffer = creatorFunction(response.output)
      responseBuffer.getBytes() == requestBuffer.getBytes()
      where:
      mimeType                  || expectedReturnMimeType    || requestBuffer                                              || replyBuffer                                                || creatorFunction
      CasualContentType.X_OCTET || CasualContentType.X_OCTET || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.FIELD   || CasualContentType.FIELD   || FieldedTypeBuffer.create().write( key, content)            || FieldedTypeBuffer.create().write( key, content)            || {bytes -> FieldedTypeBuffer.create([bytes])}
      CasualContentType.JSON    || CasualContentType.JSON    || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || {bytes -> JsonBuffer.of([bytes])}
      CasualContentType.STRING  || CasualContentType.STRING  || CStringBuffer.of(content)                                  || CStringBuffer.of(content)                                  || {bytes -> CStringBuffer.of([bytes])}
      CasualContentType.X_OCTET || CasualContentType.JSON    || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || {bytes -> JsonBuffer.of([bytes])}
      CasualContentType.JSON    || CasualContentType.X_OCTET || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.X_OCTET || CasualContentType.JSON    || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])  || {bytes -> JsonBuffer.of([bytes])}
   }

   @Unroll
   def 'local serviceCall exceptional #mimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ExceptionHandler exceptionHandler = new ExceptionHandlerImpl()
      ManagedExecutorService executorService = Mock(ManagedExecutorService){
         runAsync(_) >> {
            throw new IllegalArgumentException("very illegal")
         }
      }
      LocalRequestHandler localRequestHandler = new LocalRequestHandler()
      localRequestHandler.executorService = executorService
      CasualService casualService = new CasualService(Mock(ServiceCaller), Mock(RemoteRequestHandler), localRequestHandler, exceptionHandler, serviceRegistryLookupServiceExists)

      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.statusCode
      response.outputHeaders[HttpHeaders.CONTENT_TYPE].first.toString() == CasualContentType.NULL
      response.output.size() == 0
      where:
      mimeType                           || requestBuffer
      CasualContentType.X_OCTET          || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)])
      CasualContentType.FIELD            || FieldedTypeBuffer.create().write( key, content)
      CasualContentType.JSON             || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])
      CasualContentType.STRING           || CStringBuffer.of(content)
   }

}

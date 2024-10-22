/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources

import jakarta.ws.rs.core.Response
import org.jboss.resteasy.mock.MockDispatcherFactory
import org.jboss.resteasy.mock.MockHttpRequest
import org.jboss.resteasy.mock.MockHttpResponse
import org.jboss.resteasy.spi.Dispatcher
import se.laz.casual.api.buffer.CasualBuffer
import se.laz.casual.api.buffer.type.CStringBuffer
import se.laz.casual.api.buffer.type.JsonBuffer
import se.laz.casual.api.buffer.type.OctetBuffer
import se.laz.casual.api.buffer.type.fielded.FieldedTypeBuffer
import se.laz.casual.api.flags.AtmiFlags
import se.laz.casual.api.flags.ErrorState
import se.laz.casual.api.flags.Flag
import se.laz.casual.api.flags.ServiceReturnState
import se.laz.casual.http.resources.handlers.ExceptionHandler
import se.laz.casual.http.resources.handlers.ExceptionHandlerImpl
import se.laz.casual.http.resources.handlers.LocalRequestHandler
import se.laz.casual.http.resources.handlers.RemoteRequestHandler
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.charset.StandardCharsets

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
   def serviceRegistryLookup = Mock(ServiceRegistryLookup) {
      serviceExists(_) >> false
   }

   @Unroll
   def 'service call ok #mimeType #expectedReturnMimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            new ServiceCallResponse(ServiceReturnState.TPSUCCESS, ErrorState.OK, replyBuffer)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), Mock(ExceptionHandler), serviceRegistryLookup)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == Response.Status.OK.statusCode
      // note: contains since the for the return type, utf8 as added as encoding as well
      response.outputHeaders['content-type'].first.toString().contains(expectedReturnMimeType)
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
   def 'service call error #errorState #mimeType'()
   {
      given:
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            new ServiceCallResponse(ServiceReturnState.TPFAIL, errorState, replyBuffer)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), Mock(ExceptionHandler), serviceRegistryLookup)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == ErrorStateConverter.convert(errorState).statusCode
      response.outputHeaders['content-type'].first == (CasualContentType.NULL)
      response.output.size() == 0
      where:
      mimeType                  || errorState          || requestBuffer                                              || replyBuffer                                                || creatorFunction
      CasualContentType.X_OCTET || ErrorState.TPENOENT || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)]) || {bytes -> OctetBuffer.of([bytes])}
      CasualContentType.FIELD   || ErrorState.TPETIME  || FieldedTypeBuffer.create().write( key, content)            || FieldedTypeBuffer.create().write( key, content)            || {bytes -> FieldedTypeBuffer.create([bytes])}
   }

   @Unroll
   def 'exceptional #mimeType'()
   {
      given:
      def errorMessage = 'very illegal'
      Dispatcher dispatcher = MockDispatcherFactory.createDispatcher()
      ServiceCaller serviceCaller = Mock(ServiceCaller){
         1 * makeServiceCall(_, serviceName, Flag.of(AtmiFlags.TPNOTRAN)) >> { CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags ->
            throw new IllegalArgumentException(errorMessage)
         }
      }
      CasualService casualService = new CasualService(serviceCaller, new RemoteRequestHandler(), Mock(LocalRequestHandler), new ExceptionHandlerImpl(), serviceRegistryLookup)
      dispatcher.getRegistry().addSingletonResource(casualService)
      MockHttpRequest request = MockHttpRequest.post("${root}/${serviceName}")
              .contentType(mimeType)
              .content(requestBuffer.getBytes().first)
      MockHttpResponse response = new MockHttpResponse()
      expect:
      dispatcher.invoke(request, response)
      response.getStatus() == Response.Status.INTERNAL_SERVER_ERROR.statusCode
      response.outputHeaders['content-type'].first.toString() == CasualContentType.NULL
      response.output.size() == 0
      where:
      mimeType                           || requestBuffer
      CasualContentType.X_OCTET          || OctetBuffer.of([content.getBytes(StandardCharsets.UTF_8)])
      CasualContentType.FIELD            || FieldedTypeBuffer.create().write( key, content)
      CasualContentType.JSON             || JsonBuffer.of([content.getBytes(StandardCharsets.UTF_8)])
      CasualContentType.STRING           || CStringBuffer.of(content)
   }
}

/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources

import se.laz.casual.api.buffer.CasualBuffer
import se.laz.casual.api.buffer.ServiceReturn
import se.laz.casual.api.flags.AtmiFlags
import se.laz.casual.api.flags.ErrorState
import se.laz.casual.api.flags.Flag
import se.laz.casual.api.flags.ServiceReturnState
import se.laz.casual.connection.caller.CasualCaller
import spock.lang.Specification

class ServiceCallerImplTest extends Specification
{
   def 'ok call'()
   {
      given:
      def requestMessage = Mock(CasualBuffer)
      def replyMessage = Mock(CasualBuffer)
      def serviceReturn = new ServiceReturn(replyMessage, ServiceReturnState.TPSUCCESS, ErrorState.OK, 0)
      def serviceName = 'casual/echo'
      def flags = Flag.of(AtmiFlags.TPNOTRAN)
      def casualCaller = Mock(CasualCaller){
         1 * tpcall(serviceName, requestMessage, flags) >> serviceReturn
      }
      def serviceCaller = new ServiceCallerImpl(casualCaller)
      when:
      def reply = serviceCaller.makeServiceCall(requestMessage, serviceName, flags)
      then:
      reply.errorState() == ErrorState.OK
      reply.serviceReturnState() == ServiceReturnState.TPSUCCESS
      reply.casualBuffer() == replyMessage
   }

   def 'exceptional call'()
   {
      given:
      def requestMessage = Mock(CasualBuffer)
      def serviceName = 'casual/echo'
      def flags = Flag.of(AtmiFlags.TPNOTRAN)
      def casualCaller = Mock(CasualCaller){
         1 * tpcall(serviceName, requestMessage, flags) >> {
            throw new IllegalArgumentException("very illegal")
         }
      }
      def serviceCaller = new ServiceCallerImpl(casualCaller)
      when:
      serviceCaller.makeServiceCall(requestMessage, serviceName, flags)
      then:
      thrown(IllegalArgumentException)
   }

}

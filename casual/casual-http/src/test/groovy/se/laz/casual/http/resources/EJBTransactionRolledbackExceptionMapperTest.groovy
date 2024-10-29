/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources

import jakarta.ejb.EJBTransactionRolledbackException
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.Response
import se.laz.casual.http.resources.handlers.EJBTransactionRolledbackExceptionMapper
import spock.lang.Specification

class EJBTransactionRolledbackExceptionMapperTest extends Specification
{
   def 'ok'()
   {
      given:
      def instance = new EJBTransactionRolledbackExceptionMapper()
      when:
      Response response = instance.toResponse(new EJBTransactionRolledbackException())
      then:
      response.status == Response.Status.REQUEST_TIMEOUT.statusCode
      response.getHeaders()[HttpHeaders.CONTENT_TYPE].first.toString() == CasualContentType.NULL
      response.getHeaders()[HttpHeaders.CONTENT_LENGTH] == null
   }

}

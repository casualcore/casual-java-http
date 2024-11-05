/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import jakarta.inject.Inject;
import se.laz.casual.api.buffer.CasualBuffer;
import se.laz.casual.api.buffer.ServiceReturn;
import se.laz.casual.api.flags.AtmiFlags;
import se.laz.casual.api.flags.Flag;
import se.laz.casual.api.flags.ServiceReturnState;
import se.laz.casual.connection.caller.CasualCaller;

public class ServiceCallerImpl implements ServiceCaller
{
    private CasualCaller caller;
    @Inject
    public ServiceCallerImpl(CasualCaller caller)
    {
        this.caller = caller;
    }
    @Override
    public ServiceCallResponse makeServiceCall(CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags)
    {
        ServiceReturn<CasualBuffer> reply = caller.tpcall(serviceName, msg, flags);
        return new ServiceCallResponse(reply.getServiceReturnState(), reply.getErrorState(), (reply.getServiceReturnState() == ServiceReturnState.TPSUCCESS) ? reply.getReplyBuffer() : null);
    }
}

package se.laz.casual.http.resources.handlers;

import se.laz.casual.jca.inflow.work.CasualServiceCallWork;
import se.laz.casual.network.protocol.messages.service.CasualServiceCallRequestMessage;

import java.util.UUID;

public interface CasualServiceCallWorkCreator
{
    CasualServiceCallWork create(UUID correlationId, CasualServiceCallRequestMessage requestMessage);
}

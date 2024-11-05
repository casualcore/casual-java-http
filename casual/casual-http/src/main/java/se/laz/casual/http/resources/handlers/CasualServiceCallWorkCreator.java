/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import se.laz.casual.jca.inflow.work.CasualServiceCallWork;
import se.laz.casual.network.protocol.messages.service.CasualServiceCallRequestMessage;

import java.util.UUID;

@FunctionalInterface
public interface CasualServiceCallWorkCreator
{
    CasualServiceCallWork create(UUID correlationId, CasualServiceCallRequestMessage requestMessage);
}

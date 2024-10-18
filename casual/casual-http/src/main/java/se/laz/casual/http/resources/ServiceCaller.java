/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import se.laz.casual.api.buffer.CasualBuffer;
import se.laz.casual.api.flags.AtmiFlags;
import se.laz.casual.api.flags.Flag;

@FunctionalInterface
public interface ServiceCaller
{
    ServiceCallResponse makeServiceCall(CasualBuffer msg, String serviceName, Flag<AtmiFlags> flags);
}

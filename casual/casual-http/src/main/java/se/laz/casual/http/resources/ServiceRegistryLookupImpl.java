/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import se.laz.casual.jca.inbound.handler.service.casual.CasualServiceRegistry;

public class ServiceRegistryLookupImpl implements ServiceRegistryLookup
{
    @Override
    public boolean serviceExists(String serviceName)
    {
        return CasualServiceRegistry.getInstance().hasServiceEntry(serviceName);
    }
}

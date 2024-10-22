/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

@FunctionalInterface
public interface ServiceRegistryLookup
{
    boolean serviceExists(String serviceName);
}
/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import se.laz.casual.api.buffer.CasualBuffer;

@FunctionalInterface
public interface CasualBufferCreator
{
    CasualBuffer create(byte[] data);
}

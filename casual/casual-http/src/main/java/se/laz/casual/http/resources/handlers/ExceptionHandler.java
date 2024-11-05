/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources.handlers;

import jakarta.ws.rs.core.Response;

public interface ExceptionHandler
{
    Response handle(Exception e);
}

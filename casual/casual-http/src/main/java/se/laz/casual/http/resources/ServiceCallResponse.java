/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import se.laz.casual.api.buffer.CasualBuffer;
import se.laz.casual.api.flags.ErrorState;
import se.laz.casual.api.flags.ServiceReturnState;

import java.util.Objects;

public record ServiceCallResponse(ServiceReturnState serviceReturnState, ErrorState errorState, CasualBuffer casualBuffer)
{
    public ServiceCallResponse
    {
        Objects.requireNonNull(serviceReturnState, "serviceReturnState cannot be null");
        Objects.requireNonNull(errorState, "errorState cannot be null");
    }
}

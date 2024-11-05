/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http.resources;

import se.laz.casual.api.buffer.CasualBufferType;

public class ContentTypeConverter
{
    private ContentTypeConverter()
    {}
    public static String convert(CasualBufferType type)
    {
        return switch (type)
        {
            case JSON -> CasualContentType.JSON;
            case CSTRING -> CasualContentType.STRING;
            case FIELDED -> CasualContentType.FIELD;
            case X_OCTET -> CasualContentType.X_OCTET;
            default -> throw new IllegalArgumentException("Unsupported content type: " + type);
        };
    }
}

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
        switch (type)
        {
            case JSON: return CasualContentType.JSON;
            case CSTRING: return CasualContentType.STRING;
            case FIELDED: return CasualContentType.FIELD;
            case X_OCTET: return CasualContentType.X_OCTET;
            default:
                throw new IllegalArgumentException("Unsupported content type: " + type);
        }
    }
}

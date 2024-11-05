/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http;

import se.laz.casual.api.CasualRuntimeException;

import javax.naming.NamingException;

public class CasualNamingException extends CasualRuntimeException
{
    private static final long serialVersionUID = 1L;
    public CasualNamingException(NamingException e)
    {
        super(e);
    }
}

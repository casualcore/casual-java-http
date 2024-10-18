/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http;

import jakarta.enterprise.inject.Produces;
import se.laz.casual.connection.caller.CasualCaller;

import javax.naming.InitialContext;
import javax.naming.NamingException;

public class CasualCallerProducer
{
    @Produces
    public CasualCaller get()
    {
        try
        {
            InitialContext context = new InitialContext();
            return (CasualCaller) context.lookup("java:/global/casual-caller-app/casual-caller/CasualCallerImpl");
        }
        catch (NamingException e)
        {
            throw new MissingResourceException("Failed finding CasualCallerImpl, using CasualCaller will not work", e);
        }
    }
}

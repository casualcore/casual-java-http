/*
 * Copyright (c) 2024, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */
package se.laz.casual.http;

import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.Produces;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.logging.Logger;

public class ManagedExecutorServiceProducer
{
    private static final Logger LOG = Logger.getLogger(ManagedExecutorServiceProducer.class.getName());
    @Produces
    public ManagedExecutorService get()
    {
        return getExecutorService();
    }
    private ManagedExecutorService getExecutorService()
    {
        try
        {
            // note: In Java EE 11 we will be able to use a virtual thread managed executor service instead
            InitialContext context = new InitialContext();
            return (ManagedExecutorService) context.lookup("java:comp/DefaultManagedExecutorService");
        }
        catch (NamingException e)
        {
            LOG.warning(() -> "could not find java:comp/DefaultManagedExecutorService - application will not function");
            throw new CasualNamingException(e);
        }
    }
}

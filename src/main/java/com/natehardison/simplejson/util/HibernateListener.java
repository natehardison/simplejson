package com.natehardison.simplejson.util;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Starts/stops Hibernate on deployment/undeployment instead of doing it
 * manually.
 * Credit: https://community.jboss.org/wiki/UsingHibernateWithTomcat
 */
public class HibernateListener implements ServletContextListener {

    /**
     * Starts Hibernate on deployment.
     */
    public void contextInitialized(ServletContextEvent event) {
        HibernateManager.getManager().getSessionFactory();
    }

    /**
     * Stops Hibernate on undeployment.
     */
    public void contextDestroyed(ServletContextEvent event) {
        HibernateManager.getManager().getSessionFactory().close();
    }
}

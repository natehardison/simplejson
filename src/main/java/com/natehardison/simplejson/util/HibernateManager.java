package com.natehardison.simplejson.util;

import java.io.Serializable;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;

/**
 * The HibernateManager is a helper singleton class for performing common
 * Hibernate operations (CRUD). In particular, it insulates the client
 * from having to handle Hibernate Sessions and Transactions (and perform
 * saves, commits, rollbacks, etc.). However, it does expose the Hibernate
 * SessionFactory in case more manual control is needed.
 * @author Nate Hardison <natehardison@gmail.com>
 */
public class HibernateManager {

    private static final HibernateManager manager = new HibernateManager();
    private final SessionFactory sessionFactory;

    /**
     * Instantiates a HibernateManager, which consists mostly of building a
     * SessionFactory.
     */
    private HibernateManager() {
        try {
            // Create the SessionFactory from hibernate.cfg.xml
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");
            ServiceRegistryBuilder serviceRegistryBuilder = new ServiceRegistryBuilder();
            serviceRegistryBuilder.applySettings(configuration.getProperties());
            ServiceRegistry serviceRegistry = serviceRegistryBuilder.buildServiceRegistry();
            this.sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (Throwable ex) {
            // Log the exception so it's not swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * @return The singleton instance of the HibernateManager.
     */
    public static HibernateManager getManager() {
        return manager;
    }

    /**
     * A SessionFactory allows manual access to Hibernate in case more control
     * is needed than the CRUD methods provide.
     * @return The singleton instance of the Hibernate SessionFactory.
     */
    public SessionFactory getSessionFactory() {
        return manager.sessionFactory;
    }

    /**
     * Retrieves all of the resources of type klass from the Hibernate session.
     * @param klass The type of resources to retrieve.
     * @return A list of resource objects.
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getResources(Class<T> klass) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        List<T> resources = null;
        try {
            tx = session.beginTransaction();
            // bit of a hack, but Class#getSimpleName gets us the table name
            // assuming that we set things up appropriately
            resources = (List<T>) session.createQuery("from " + klass.getSimpleName()).list();
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            // this exception could be used by the caller to determine what
            // exactly caused the problem and return the appropriate HTTP code
            throw e;
        }
        return resources;
    }

    /**
     * Retrieves the resource of type klass with ID id. Returns null if the
     * resource does not exist.
     * @param klass
     * @param id
     * @return
     */
    public Object getResource(Class<?> klass, Serializable id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        Object resource = null;
        try {
            tx = session.beginTransaction();
            // use Session#get, not Session#load to avoid an exception if the
            // resource doesn't exist
            resource = session.get(klass, id);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
        return resource;
    }

    /**
     * Creates a record for the provided resource in Hibernate. Throws an
     * exception if a resource with the same ID already exists.
     * @param resource The resource to save in Hibernate.
     * @return The ID of the saved resource.
     */
    public Serializable createResource(Object resource) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        Serializable id = null;
        try {
            tx = session.beginTransaction();
            // will throw an exception if the resource exists
            id = session.save(resource);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                session.getTransaction().rollback();
            }
            throw e;
        }
        return id;
    }

    /**
     * Updates the resource of type klass with ID id to match the provided
     * resource. If a resource matching klass and id doesn't exist in
     * Hibernate, then this method throws an exception.
     * @param resource The updated resource data.
     * @param klass The type of resource to update.
     * @param id The id of the resource to update.
     */
    public void updateResource(Object resource, Class<?> klass, Serializable id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(resource);
            tx.commit();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
    }

    /**
     * Checks to see if a resource of type klass and with ID id exists, and if
     * so, updates that resource to match the provided resource. Otherwise,
     * creates a new resource. Returns a boolean to inform the caller whether
     * or not a new resource was created.
     * @param resource The resource data to update or create.
     * @param klass The type of the resource to update.
     * @param id The id of the resource to update.
     * @return true if a new resource was created, false if an update occurred.
     */
    public boolean createOrUpdateResource(Object resource, Class<?> klass, Serializable id) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        boolean created = false;
        try {
            tx = session.beginTransaction();
            // This is a great place to use Session#saveOrUpdate. Unfortunately,
            // that method doesn't tell us whether or not the resource was
            // created, which is needed for the proper PUT response code.
            Object existingResource = session.get(klass, id, LockOptions.UPGRADE);
            if (existingResource == null) {
                session.save(resource);
                created = true;
            } else {
                session.merge(resource);
            }
            tx.commit();
        } catch (HibernateException e) {
            e.printStackTrace();
            if (tx != null) {
                tx.rollback();
            }
            throw e;
        }
        return created;
    }

}

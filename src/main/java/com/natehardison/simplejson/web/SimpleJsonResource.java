package com.natehardison.simplejson.web;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.HibernateException;
import org.hibernate.StaleStateException;

import com.natehardison.simplejson.domain.Owner;
import com.natehardison.simplejson.domain.Person;
import com.natehardison.simplejson.util.HibernateManager;
import com.natehardison.simplejson.util.SimpleJsonUtil;

/**
 * Base RESTful resource class. Supported operations:
 * - GET  /{class}      => get all resources of type class
 * - GET  /{class}/{id} => get resource of type class with ID id
 * - POST /{class}      => create new resource of type class
 * - POST /{class}/{id} => update car with specified VIN
 * - PUT  /{class}/{id} => create or update car with specified VIN
 * @author Nate Hardison <natehardison@gmail.com>
 */
@Path("/{class: car|owner|person|truck}")
public class SimpleJsonResource {

    @Context UriInfo uriInfo;

    /**
     * Retrieves all resources of type className.
     * @param className The type of resource to retrieve.
     * @return A list of all resources of type className
     */
    @SuppressWarnings("unchecked")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Object> getResources(@PathParam("class") String className) {
        // get the class object matching the path
        Class<?> klass;
        try {
            klass = SimpleJsonUtil.getModelClassForName(className);
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(500);
        }

        List<Object> resources;
        try {
            resources = (List<Object>) HibernateManager.getManager().getResources(klass);
        } catch (HibernateException e) {
            // from GETs, this is the only way to return a 400
            // assume the request was botched client-side ;-)
            throw new WebApplicationException(400);
        }

        return resources;
    }

    /**
     * Retrieves the resource of type className specified by id.
     * @param className The type of the resource.
     * @param id The unique id of the resource.
     * @return The record of the matching resource.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getResource(@PathParam("class") String className, @PathParam("id") String id) {
        Class<?> klass;
        try {
            klass = SimpleJsonUtil.getModelClassForName(className);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new WebApplicationException(500);
        }

        Object resource;
        try {
            // this is our ugly hack because it turns out that the Person IDs
            // are longs, but Vehicle VINs are Strings. The way to fix would
            // be to have nearly-identical methods for Persons and Vehicles
            // differing only in their signature. This way seems better.
            if (klass == Owner.class || klass == Person.class) {
                resource = HibernateManager.getManager().getResource(klass, Long.decode(id));
            } else {
                resource = HibernateManager.getManager().getResource(klass, id);
            }
        } catch (NumberFormatException e) {
            throw new WebApplicationException(400);
        } catch (HibernateException e) {
            throw new WebApplicationException(400);
        }

        // if we couldn't find the resource, then return a 404
        if (resource == null) {
            throw new WebApplicationException(404);
        }

        // this will return HTTP 200
        return resource;
    }

    /**
     * Creates a new resource of type className based on the provided JSON
     * entity. If a resource of that type already exists with the same id, an
     * HTTP 400 is returned.
     * @param className The type of the resource.
     * @param resourceJson The new resource to create.
     * @return HTTP 201 (Created) along with the URI of the new resource if all goes well.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createResource(@PathParam("class") String className, String resourceJson) {
        Class<?> klass;
        try {
            klass = SimpleJsonUtil.getModelClassForName(className);
        } catch (ClassNotFoundException e) {
            return Response.status(500).build();
        }

        // parse the resourceJson into its corresponding object
        Object resource;
        ObjectMapper om = new ObjectMapper();
        try {
            resource = om.readValue(resourceJson, klass);
        } catch (JsonParseException e) {
            return Response.status(400).build();
        } catch (JsonMappingException e) {
            return Response.status(400).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }

        Serializable id = null;
        try {
            id = HibernateManager.getManager().createResource(resource);
        } catch (HibernateException e) {
            return Response.status(400).build();
        }

        // tack on the new resource's ID to the request URI to get its location
        URI location = UriBuilder.fromUri(uriInfo.getRequestUri()).path(id.toString()).build();
        return Response.created(location).build();
    }

    /**
     * Updates the resource of type className specified by id to match the
     * provided JSON entity. If no resource exists with the same id, an HTTP
     * 404 (Not Found) is returned.
     * @param className The type of the resource.
     * @param id The id of the resource.
     * @param resourceJson The resource data to update.
     * @return HTTP 204 (No Content) if all goes well.
     */
    @POST
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResource(@PathParam("class") String className, @PathParam("id") String id, String resourceJson) {
        Class<?> klass;
        try {
            klass = SimpleJsonUtil.getModelClassForName(className);
        } catch (ClassNotFoundException e) {
            return Response.status(500).build();
        }

        Object resource;
        ObjectMapper om = new ObjectMapper();
        try {
            resource = om.readValue(resourceJson, klass);
        } catch (JsonParseException e) {
            return Response.status(400).build();
        } catch (JsonMappingException e) {
            return Response.status(400).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }

        try {
            if (klass == Owner.class || klass == Person.class) {
                // hack hack hack...because Session#update needs the resource
                // to have its ID already set
                ((Person) resource).setId(Long.decode(id));
                HibernateManager.getManager().updateResource(resource, klass, Long.decode(id));
            } else {
                HibernateManager.getManager().updateResource(resource, klass, id);
            }
        } catch (NumberFormatException e) {
            return Response.status(400).build();
        } catch (StaleStateException e) {
            // this means that update couldn't find a match for the ID
            return Response.status(404).build();
        } catch (HibernateException e) {
            return Response.status(400).build();
        }
        return Response.noContent().build();
    }

    /**
     * Creates or updates the resource of type className specified by id to
     * match the provided JSON entity.
     * @param className The type of the resource.
     * @param id The id of the resource.
     * @param resourceJson The resource data to create or update.
     * @return HTTP 201 (Created) if a new resource is created, 204 otherwise.
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrUpdateResource(@PathParam("class") String className, @PathParam("id") String id, String resourceJson) {
        Class<?> klass;
        try {
            klass = SimpleJsonUtil.getModelClassForName(className);
        } catch (ClassNotFoundException e) {
            return Response.status(500).build();
        }

        Object resource;
        ObjectMapper om = new ObjectMapper();
        try {
            resource = om.readValue(resourceJson, klass);
        } catch (JsonParseException e) {
            return Response.status(400).build();
        } catch (JsonMappingException e) {
            return Response.status(400).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }

        // bit of a hack to distinguish between create and update, as per RFC 2616
        boolean created;
        try {
            if (klass == Owner.class || klass == Person.class) {
                // TODO: remove this ugly hack
                ((Person) resource).setId(Long.decode(id));
                created = HibernateManager.getManager().createOrUpdateResource(resource, klass, Long.decode(id));
            } else {
                created = HibernateManager.getManager().createOrUpdateResource(resource, klass, id);
            }
        } catch (NumberFormatException e) {
            return Response.status(400).build();
        } catch (HibernateException e) {
            return Response.status(400).build();
        }

        if (created) {
            URI location = UriBuilder.fromUri(uriInfo.getRequestUri()).path(id).build();
            return Response.created(location).build();
        }
        return Response.noContent().build();
    }

}

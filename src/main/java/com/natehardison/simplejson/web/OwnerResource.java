package com.natehardison.simplejson.web;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.natehardison.simplejson.domain.Owner;
import com.natehardison.simplejson.domain.Vehicle;
import com.natehardison.simplejson.util.HibernateManager;
import com.natehardison.simplejson.util.SimpleJsonUtil;

/**
 * Resource class for dealing with an Owner's vehicles. Supported operations:
 * - GET  /owner/{id}/vehicles                  => get owner's vehicles
 * - POST /owner/{id}/vehicles?type=[car,truck] => add vehicle to owner
 * @author Nate Hardison <natehardison@gmail.com>
 */
@Path("/owner/{id}/vehicles")
public class OwnerResource {

    @Context UriInfo uriInfo;

    /**
     * Retrieve the vehicles belonging to the Owner with ID id.
     * @param id The id of the Owner.
     * @return A Set of vehicles.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Vehicle> getVehicles(@PathParam("id") long id) {
        Set<Vehicle> vehicles;
        // we'll do this "manually" since it's a somewhat complex query
        Session session = HibernateManager.getManager().getSessionFactory().getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Owner owner = (Owner) session.load(Owner.class, id);
            vehicles = owner.getVehicles();
            tx.commit();
        } catch (ObjectNotFoundException e) {
            // in case the owner isn't found, return a 404
            if (tx != null) {
                tx.rollback();
            }
            throw new WebApplicationException(404);
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            throw new WebApplicationException(400);
        }
        return vehicles;
    }

    /**
     * Adds a Vehicle to the Owner's collection. If the Vehicle doesn't exist
     * in Hibernate, then an instance is created.
     * @param id The id of the Owner.
     * @param vehicleClassName The type of vehicle (e.g., car, truck).
     * @param vehicleJson The vehicle data in a JSON string.
     * @return HTTP 201 if a vehicle was created, 204 otherwise.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVehicle(@PathParam("id") long id,
                                  @QueryParam("type") String vehicleClassName,
                                  String vehicleJson) {
        Class<?> vehicleClass;
        try {
            vehicleClass = SimpleJsonUtil.getModelClassForName(vehicleClassName);
        } catch (ClassNotFoundException e) {
            throw new WebApplicationException(400);
        }

        Vehicle vehicle;
        ObjectMapper om = new ObjectMapper();
        try {
            vehicle = (Vehicle) om.readValue(vehicleJson, vehicleClass);
        } catch (JsonParseException e) {
            return Response.status(400).build();
        } catch (JsonMappingException e) {
            return Response.status(400).build();
        } catch (IOException e) {
            return Response.status(500).build();
        }

        Session session = HibernateManager.getManager().getSessionFactory().getCurrentSession();
        Transaction tx = null;
        boolean created = false;
        try {
            tx = session.beginTransaction();
            Owner owner = (Owner) session.load(Owner.class, id);
            // use Session#get to avoid exception if vehicle doesn't exist
            Vehicle existingVehicle = (Vehicle) session.get(Vehicle.class, vehicle.getVin());
            if (existingVehicle == null) {
                session.save(vehicle);
                created = true;
            }
            owner.getVehicles().add(vehicle);
            tx.commit();
        } catch (ObjectNotFoundException e) {
            if (tx != null) {
                tx.rollback();
            }
            return Response.status(404).build();
        } catch (HibernateException e) {
            if (tx != null) {
                tx.rollback();
            }
            return Response.status(400).build();
        }

        if (created) {
            UriBuilder uriBuilder = uriInfo.getBaseUriBuilder().path("{class}/{id}");
            URI location = uriBuilder.build(vehicleClassName.toLowerCase(), vehicle.getVin());
            return Response.created(location).build();
        }
        return Response.noContent().build();
    }

}

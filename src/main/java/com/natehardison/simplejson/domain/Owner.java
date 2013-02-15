package com.natehardison.simplejson.domain;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model class for a simple Owner entity.
 * @author Nate Hardison <natehardison@gmail.com>
 */
@XmlRootElement
public class Owner extends Person {

    /**
     * For simplicity, we'll store an owner's vehicles as a hash set. A
     * (better?) way would be to use a Map or a List, which would allow
     * access to a particular vehicle belonging to a particular owner,
     * but it would complicate the DB schema.
     */
    protected Set<Vehicle> vehicles = new HashSet<Vehicle>();

    // empty constructor needed for Hibernate
    public Owner() {}

    public Set<Vehicle> getVehicles() { return this.vehicles; }
    public void setVehicles(Set<Vehicle> vehicles) { this.vehicles = vehicles; }

}

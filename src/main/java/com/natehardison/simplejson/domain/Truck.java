package com.natehardison.simplejson.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model class for a simple Truck entity.
 * @author Nate Hardison <natehardison@gmail.com>
 */
@XmlRootElement
public class Truck extends Vehicle {

    private int axles;

    // empty constructor needed for Hibernate
    public Truck() {}

    public int getAxles() { return this.axles; }
    public void setAxles(int axles) { this.axles = axles; }

}

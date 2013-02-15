package com.natehardison.simplejson.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model class for a Car entity.
 * @author Nate Hardison <natehardison@gmail.com>
 */
@XmlRootElement
public class Car extends Vehicle {

    private int doors;

    // empty constructor needed for Hibernate
    public Car() {}

    public int getDoors() { return this.doors; }
    public void setDoors(int doors) { this.doors = doors; }

}

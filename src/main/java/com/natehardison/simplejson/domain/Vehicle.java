package com.natehardison.simplejson.domain;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Model class for a simple Vehicle entity.
 * @author Nate Hardison <natehardison@gmail.com>
 */
@XmlRootElement
public abstract class Vehicle {

    // we use a String and not an integral type here because VINs have letters
    // http://www.nhtsa.gov/Vehicle+Safety/Vehicle-Related+Theft/Vehicle+Identification+Numbers+%28VINs%29
    // we'll also assume that this will NOT be auto-generated by the DB
    protected String vin;

    // we'll use an int for simplicity
    protected int fuelConsumption;

    // assume horsepower is an integral quantity
    protected int horsepower;

    // we'll assume miles per hour are integral
    protected int speed;

    // empty constructor needed for Hibernate
    public Vehicle() {}

    public String getVin() { return this.vin; }
    public void setVin(String vin) { this.vin = vin; }

    public int getFuelConsumption() { return this.fuelConsumption; }
    public void setFuelConsumption(int fuelConsumption) { this.fuelConsumption = fuelConsumption; }

    public int getHorsepower() { return this.horsepower; }
    public void setHorsepower(int horsepower) { this.horsepower = horsepower; }

    public int getSpeed() { return this.speed; }
    public void setSpeed(int speed) { this.speed = speed; }

}

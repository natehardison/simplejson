package com.natehardison.simplejson.util;

/**
 * Simple utility class for helper functions common to multiple classes.
 * @author Nate Hardison <natehardison@gmail.com>
 */
public class SimpleJsonUtil {
    /**
     * The package name of where the model classes are stored.
     */
    private static final String MODEL_PACKAGE_PREFIX = "com.natehardison.simplejson.domain.";

    /**
     * Turn a simple model class name into an instance of the model Class
     * object. Throws ClassNotFoundException if the class cannot be found.
     * @param className The model class name (e.g., "car", "truck")
     * @return The corresponding model class (e.g., Car.class, Truck.class)
     * @throws ClassNotFoundException
     */
    public static Class<?> getModelClassForName(String className) throws ClassNotFoundException {
        className = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
        return Class.forName(MODEL_PACKAGE_PREFIX + className);
    }

}

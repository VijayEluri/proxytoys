/*
 * (c) 2003-2005, 2009, 2010 ThoughtWorks Ltd
 * All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 11-May-2004
 */
package com.thoughtworks.proxy.kit;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.thoughtworks.proxy.ProxyFactory;
import com.thoughtworks.proxy.factory.InvokerReference;

/**
 * Helper class for introspecting interface and class hierarchies.
 *
 * @author Aslak Helles&oslash;y
 * @author J&ouml;rg Schaible
 * @since 0.2
 */
public class ReflectionUtils {

    /**
     * the {@link Object#equals(Object)} method.
     */
    public static final Method equals;
    /**
     * the {@link Object#hashCode()} method.
     */
    public static final Method hashCode;
    /**
     * the {@link Object#toString()} method.
     */
    public static final Method toString;

    static {
        try {
            equals = Object.class.getMethod("equals", new Class[]{Object.class});
            hashCode = Object.class.getMethod("hashCode");
            toString = Object.class.getMethod("toString");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e.toString());
        }
    }

    /**
     * Constructor. Do not call, it is a factory.
     */
    private ReflectionUtils() {
    }

    /**
     * Get all the interfaces implemented by a list of objects.
     *
     * @param objects the list of objects to consider.
     * @return an set of interfaces. The set may be empty
     * @since 0.2
     */
    public static Set<Class<?>> getAllInterfaces(Object... objects) {
        final Set<Class<?>> interfaces = new HashSet<Class<?>>();
        for (Object object : objects) {
            if (object != null) {
                getInterfaces(object.getClass(), interfaces);
            }
        }
        interfaces.remove(InvokerReference.class);
        return interfaces;
    }

    /**
     * Get all interfaces of the given type. If the type is a class, the returned set contains any interface, that is
     * implemented by the class. If the type is an interface, the all superinterfaces and the interface itself are
     * included.
     *
     * @param type type to explore.
     * @return a {@link Set} with all interfaces. The set may be empty.
     * @since 0.2
     */
    public static Set<Class<?>> getAllInterfaces(final Class<?> type) {
        final Set<Class<?>> interfaces = new HashSet<Class<?>>();
        getInterfaces(type, interfaces);
        interfaces.remove(InvokerReference.class);
        return interfaces;
    }

    private static void getInterfaces(Class<?> type, final Set<Class<?>> interfaces) {
        if (type.isInterface()) {
            interfaces.add(type);
        }
        // Class.getInterfaces will return only the interfaces that are
        // implemented by the current class. Therefore we must loop up
        // the hierarchy for the superclasses and the superinterfaces.
        while (type != null) {
            for (Class<?> anImplemented : type.getInterfaces()) {
                if (!interfaces.contains(anImplemented)) {
                    getInterfaces(anImplemented, interfaces);
                }
            }
            type = type.getSuperclass();
        }
    }

    /**
     * Get most common superclass for all given objects.
     *
     * @param objects the array of objects to consider.
     * @return the superclass or <code>{@link Void Void.class}</code> for an empty array.
     * @since 0.2
     */
    public static Class<?> getMostCommonSuperclass(Object... objects) {
        Class<?> type = null;
        boolean found = false;
        if (objects != null && objects.length > 0) {
            while (!found) {
                for (Object object : objects) {
                    found = true;
                    if (object != null) {
                        final Class<?> currenttype = object.getClass();
                        if (type == null) {
                            type = currenttype;
                        }
                        if (!type.isAssignableFrom(currenttype)) {
                            if (currenttype.isAssignableFrom(type)) {
                                type = currenttype;
                            } else {
                                type = type.getSuperclass();
                                found = false;
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (type == null) {
            type = Object.class;
        }
        return type;
    }

    /**
     * Add the given type to the set of interfaces, if the given ProxyFactory supports proxy generation for this type.
     *
     * @param type        the class type (<code>Object.class</code> will be ignored)
     * @param interfaces   the set of interfaces
     * @param proxyFactory the {@link ProxyFactory} in use
     * @since 0.2
     */
    public static void addIfClassProxyingSupportedAndNotObject(
            final Class<?> type, final Set<Class<?>> interfaces, final ProxyFactory proxyFactory) {
        if (proxyFactory.canProxy(type) && !type.equals(Object.class)) {
            interfaces.add(type);
        }
    }

    /**
     * Get the method of the given type, that has matching parameter types to the given arguments.
     *
     * @param type       the type
     * @param methodName the name of the method to search
     * @param args       the arguments to match
     * @return the matching {@link Method}
     * @throws NoSuchMethodException if no matching {@link Method} exists
     * @since 0.2
     */
    public static Method getMatchingMethod(final Class<?> type, final String methodName, final Object[] args)
            throws NoSuchMethodException {
        final Object[] newArgs = args == null ? new Object[0] : args;
        final Method[] methods = type.getMethods();
        final Set<Method> possibleMethods = new HashSet<Method>();
        Method method = null;
        for (int i = 0; method == null && i < methods.length; i++) {
            if (methodName.equals(methods[i].getName())) {
                final Class<?>[] argTypes = methods[i].getParameterTypes();
                if (argTypes.length == newArgs.length) {
                    boolean exact = true;
                    Method possibleMethod = methods[i];
                    for (int j = 0; possibleMethod != null && j < argTypes.length; j++) {
                        final Class<?> newArgType = newArgs[j] != null ? newArgs[j].getClass() : Object.class;
                        if ((argTypes[j].equals(byte.class) && newArgType.equals(Byte.class))
                                || (argTypes[j].equals(char.class) && newArgType.equals(Character.class))
                                || (argTypes[j].equals(short.class) && newArgType.equals(Short.class))
                                || (argTypes[j].equals(int.class) && newArgType.equals(Integer.class))
                                || (argTypes[j].equals(long.class) && newArgType.equals(Long.class))
                                || (argTypes[j].equals(float.class) && newArgType.equals(Float.class))
                                || (argTypes[j].equals(double.class) && newArgType.equals(Double.class))
                                || (argTypes[j].equals(boolean.class) && newArgType.equals(Boolean.class))) {
                            exact = true;
                        } else if (!argTypes[j].isAssignableFrom(newArgType)) {
                            possibleMethod = null;
                            exact = false;
                        } else if (!argTypes[j].isPrimitive()) {
                            if (!argTypes[j].equals(newArgType)) {
                                exact = false;
                            }
                        }
                    }
                    if (exact) {
                        method = possibleMethod;
                    } else if (possibleMethod != null) {
                        possibleMethods.add(possibleMethod);
                    }
                }
            }
        }
        if (method == null && possibleMethods.size() > 0) {
            method = possibleMethods.iterator().next();
        }
        if (method == null) {
            final StringBuilder name = new StringBuilder(type.getName());
            name.append('.');
            name.append(methodName);
            name.append('(');
            for (int i = 0; i < newArgs.length; i++) {
                if (i != 0) {
                    name.append(", ");
                }
                name.append(newArgs[i].getClass().getName());
            }
            name.append(')');
            throw new NoSuchMethodException(name.toString());
        }
        return method;
    }

    /**
     * Write a {@link Method} into an {@link ObjectOutputStream}.
     *
     * @param out    the stream
     * @param method the {@link Method} to write
     * @throws IOException if writing causes a problem
     * @since 0.2
     */
    public static void writeMethod(final ObjectOutputStream out, final Method method) throws IOException {
        out.writeObject(method.getDeclaringClass());
        out.writeObject(method.getName());
        out.writeObject(method.getParameterTypes());
    }

    /**
     * Read a {@link Method} from an {@link ObjectInputStream}.
     *
     * @param in the stream
     * @return the read {@link Method}
     * @throws IOException            if reading causes a problem
     * @throws ClassNotFoundException if class types from objects of the InputStream cannot be found
     * @throws InvalidObjectException if the {@link Method} cannot be found
     * @since 0.2
     */
    public static Method readMethod(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        final Class<?> type = Class.class.cast(in.readObject());
        final String name = String.class.cast(in.readObject());
        final Class<?>[] parameters = Class[].class.cast(in.readObject());
        try {
            return type.getMethod(name, parameters);
        } catch (final NoSuchMethodException e) {
            throw new InvalidObjectException(e.getMessage());
        }
    }

    /**
     * Create an array of types.
     * 
     * @param primaryType the primary types
     * @param types the additional types (may be null)
     * @return an array of all the given types with the primary type as first element
     * @since 1.0
     */
    public static Class<?>[] makeTypesArray(Class<?> primaryType, Class<?>[] types) {
        if (primaryType == null) {
            return types;
        }
        Class<?>[] retVal = new Class[types == null ? 1 : types.length +1];
        retVal[0] = primaryType;
        if (types != null) {
            System.arraycopy(types, 0, retVal, 1, types.length);
        }
        return retVal;
    }
}

/*
 * (c) 2003-2005, 2009, 2010 ThoughtWorks Ltd. All rights reserved.
 * (c) 2015 ProxyToys Committers. All rights reserved.
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 * 
 * Created on 14-May-2004
 */
package com.thoughtworks.proxy.toys.hotswap;

import static com.thoughtworks.proxy.toys.delegate.DelegationMode.DIRECT;
import static com.thoughtworks.proxy.toys.delegate.DelegationMode.SIGNATURE;

import com.thoughtworks.proxy.ProxyFactory;
import com.thoughtworks.proxy.factory.StandardProxyFactory;
import com.thoughtworks.proxy.kit.ObjectReference;
import com.thoughtworks.proxy.kit.ReflectionUtils;
import com.thoughtworks.proxy.kit.SimpleReference;
import com.thoughtworks.proxy.toys.delegate.DelegationMode;


/**
 * Factory for proxy instances that allow to exchange the delegated instance. Every created proxy will implement
 * {@link Swappable}, that is used for the hot swap operation.
 *
 * @author Dan North
 * @author Aslak Helles&oslash;y
 * @author J&ouml;rg Schaible
 * @author Conrad Benham
 * @author Paul Hammant
 * @see com.thoughtworks.proxy.toys.hotswap
 * @since 0.1
 */
public class HotSwapping<T> {

    private Object instance;
    private Class<?>[] types;
    private DelegationMode delegationMode;

    private HotSwapping(final Class<T> primaryType, Class<?>... types) {
        this.types = ReflectionUtils.makeTypesArray(primaryType, types);
    }

    /**
     * Creates a factory for proxy instances that allow the exchange of delegated instances.
     *
     * @param type the type of the proxy when it is finally created.
     * @param <T> the proxied type
     * @return a factory that will proxy instances of the supplied type.
     * @since 1.0
     */
    public static <T> HotSwappingWith<T> proxy(final Class<T> type) {
        return new HotSwappingWith<T>(new HotSwapping<T>(type));
    }
    
    /**
     * Creates a factory for proxy instances that allow the exchange of delegated instances.
     *
     * @param primaryType the primary type implemented by the proxy
     * @param types other types that are implemented by the proxy
     * @param <T> the proxied type
     * @return a factory that will proxy instances of the supplied type.
     * @since 1.0
     */
    public static <T> HotSwappingWith<T> proxy(final Class<T> primaryType, final Class<?> ... types) {
        return new HotSwappingWith<T>(new HotSwapping<T>(primaryType, types));
    }

    /**
     * Create a proxy with hot swapping capabilities for specific types of the delegate given with an
     * {@link ObjectReference}. The delegate must implement the given types, if the invoker is in static typing mode,
     * otherwise it must only have signature compatible methods. Proxies created by this method will implement
     * {@link Swappable}
     *
     * @param factory the {@link ProxyFactory} to use.
     * @return the created proxy implementing the <tt>types</tt> and {@link Swappable}
     * @since 1.0
     */
    private T build(final ProxyFactory factory) {
        final ObjectReference<Object> delegateReference = new SimpleReference<Object>(instance);
        return new HotSwappingInvoker<T>(types, factory, delegateReference, delegationMode).proxy();
    }

    public static class HotSwappingWith<T> {
        private final HotSwapping<T> hotswapping;

        public HotSwappingWith(HotSwapping<T> hotswapping) {
            this.hotswapping = hotswapping;
        }

        /**
         * Defines the object that shall be proxied. This delegate must implement the types used to create the hot swap or
         * have signature compatible methods.
         *
         * @param instance the object that shall be proxied.
         * @return the factory that will proxy instances of the supplied type.
         * @since 1.0
         */
        public HotSwappingBuildOrMode<T> with(final Object instance) {
            hotswapping.instance = instance;
            hotswapping.delegationMode = DIRECT;
            for (Class<?> type : hotswapping.types) {
                if (!type.isInstance(instance)) {
                    hotswapping.delegationMode = SIGNATURE;
                    break;
                }
            }
            return new HotSwappingBuildOrMode<T>(hotswapping);
        }
    }

    public static class HotSwappingBuildOrMode<T> extends HotSwappingBuild<T>{
        public HotSwappingBuildOrMode(HotSwapping<T> hotswapping) {
            super(hotswapping);
        }

        /**
         * Forces a particular delegation mode to be used.
         *
         * @param delegationMode refer to {@link DelegationMode#DIRECT} or
         *                       {@link DelegationMode#SIGNATURE} for allowed
         *                       values.
         * @return the factory that will proxy instances of the supplied type.
         * @since 1.0
         */
        public HotSwappingBuild<T> mode(DelegationMode delegationMode) {
            hotswapping.delegationMode = delegationMode;
            return new HotSwappingBuild<T>(hotswapping);
        }
    }

    public static class HotSwappingBuild<T> {
        protected final HotSwapping<T> hotswapping;

        public HotSwappingBuild(HotSwapping<T> hotswapping) {
            this.hotswapping = hotswapping;
        }

        /**
         * Create a proxy with hot swapping capabilities for specific types of the delegate given with an
         * {@link ObjectReference}. The delegate must implement the given types, if the invoker is in static typing mode,
         * otherwise it must only have signature compatible methods. Proxies created by this method will implement
         * {@link Swappable}
         *
         * @return the created proxy implementing the <tt>types</tt> and {@link Swappable}
         * @see com.thoughtworks.proxy.toys.hotswap
         * @since 1.0
         */
        public T build() {
            return build(new StandardProxyFactory());
        }
        
        /**
         * Create a proxy with hot swapping capabilities for specific types of the delegate given with an
         * {@link ObjectReference}. The delegate must implement the given types, if the invoker is in static typing mode,
         * otherwise it must only have signature compatible methods. Proxies created by this method will implement
         * {@link Swappable}
         *
         * @param factory the {@link ProxyFactory} to use.
         * @return the created proxy implementing the <tt>types</tt> and {@link Swappable}
         * @see com.thoughtworks.proxy.toys.hotswap
         * @since 1.0
         */
        public T build(final ProxyFactory factory) {
            return hotswapping.build(factory);
        }
    }
}

/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.factory;

import java.awt.RenderingHints;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.naming.Name;
import javax.sql.DataSource;

import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.util.InternationalString;


/**
 * A set of hints providing control on factories to be used. Those hints are typically used by
 * renderers or {@linkplain org.opengis.coverage.processing.GridCoverageProcessor grid coverage
 * processors} for example. They provides a way to control low-level details. Example:
 * <p>
 * <blockquote><pre>
 * CoordinateOperationFactory myFactory = &amp;hellip
 * Hints hints = new Hints(Hints.{@linkplain #COORDINATE_OPERATION_FACTORY}, myFactory);
 * AbstractProcessor processor = new DefaultProcessor(hints);
 * </pre></blockquote>
 * <p>
 * Any hint mentioned by this class is considered to be API, failure to make
 * use of a hint by a GeoTools factory implementation is considered a bug (as
 * it will prevent the use of this library for application specific tasks).
 * <p>
 * When hints are used in conjunction with the {@linkplain FactoryRegistry factory service
 * discovery mechanism} we have the complete geotools plugin system. By using hints to
 * allow application code to effect service discovery we allow client code to
 * retarget the geotools library for their needs.
 *
 * @since 2.1
 *
 * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
 * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
 * @author Martin Desruisseaux
 * @author Jody Garnett
 */
public class Hints extends RenderingHints {
    /**
     * A set of system-wide hints to use by default.
     */
    private static final Hints GLOBAL = new Hints();

    /**
     * {@code true} if {@link #scanSystemProperties} needs to be invoked.
     */
    private static boolean needScan = true;



    ////////////////////////////////////////////////////////////////////////
    ////////                                                        ////////
    ////////              Coordinate Reference Systems              ////////
    ////////                                                        ////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * The {@link org.opengis.referencing.crs.CRSAuthorityFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getCRSAuthorityFactory
     */
    public static final ClassKey CRS_AUTHORITY_FACTORY = new ClassKey(
            "org.opengis.referencing.crs.CRSAuthorityFactory");

    /**
     * The {@link org.opengis.referencing.cs.CSAuthorityFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getCSAuthorityFactory
     */
    public static final ClassKey CS_AUTHORITY_FACTORY = new ClassKey(
            "org.opengis.referencing.cs.CSAuthorityFactory");

    /**
     * The {@link org.opengis.referencing.datum.DatumAuthorityFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getDatumAuthorityFactory
     */
    public static final ClassKey DATUM_AUTHORITY_FACTORY = new ClassKey(
            "org.opengis.referencing.datum.DatumAuthorityFactory");

    /**
     * The {@link org.opengis.referencing.crs.CRSFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getCRSFactory
     */
    public static final ClassKey CRS_FACTORY = new ClassKey(
            "org.opengis.referencing.crs.CRSFactory");

    /**
     * The {@link org.opengis.referencing.cs.CSFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getCSFactory
     */
    public static final ClassKey CS_FACTORY = new ClassKey(
            "org.opengis.referencing.cs.CSFactory");

    /**
     * The {@link org.opengis.referencing.datum.DatumFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getDatumFactory
     */
    public static final ClassKey DATUM_FACTORY = new ClassKey(
            "org.opengis.referencing.datum.DatumFactory");

    /**
     * The {@link org.opengis.referencing.operation.CoordinateOperationFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getCoordinateOperationFactory
     */
    public static final ClassKey COORDINATE_OPERATION_FACTORY = new ClassKey(
            "org.opengis.referencing.operation.CoordinateOperationFactory");

    /**
     * The {@link org.opengis.referencing.operation.CoordinateOperationAuthorityFactory} instance
     * to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getCoordinateOperationAuthorityFactory
     */
    public static final ClassKey COORDINATE_OPERATION_AUTHORITY_FACTORY = new ClassKey(
            "org.opengis.referencing.operation.CoordinateOperationAuthorityFactory");

    /**
     * The {@link org.opengis.referencing.operation.MathTransformFactory} instance to use.
     *
     * @see org.geotools.referencing.FactoryFinder#getMathTransformFactory
     */
    public static final ClassKey MATH_TRANSFORM_FACTORY = new ClassKey(
            "org.opengis.referencing.operation.MathTransformFactory");

    /**
     * Used to direct WKT CRS Authority to a directory containing extra definitions.
     * The value should be an instance of {@link File} or {@link String} refering to
     * an existing directory.
     * <p>
     * Filenames in the supplied directory should be of the form
     * <code><var>authority</var>.properties</code> where <var>authority</var>
     * is the authority name space to use. For example the
     * {@value org.geotools.referencing.factory.epsg.FactoryUsingWKT#FILENAME}
     * file contains extra CRS to add as new EPSG codes.
     * <p>
     * To set the directory on the command line:
     *
     * <blockquote><pre>
     * -D{@value GeoTools#CRS_AUTHORITY_EXTRA_DIRECTORY}=<var>path</var>
     * </pre></blockquote>
     *
     * @since 2.4
     */
    public static final FileKey CRS_AUTHORITY_EXTRA_DIRECTORY = new FileKey(false);

    /**
     * The {@linkplain javax.sql.DataSource data source} name to lookup from JNDI when
     * initializing the {@linkplain org.geotools.referencing.factory.epsg EPSG factory}.
     * Possible values:
     * <ul>
     *   <li>{@link String} - used with JNDI to locate datasource. This hint has no effect if
     *       there is no {@linkplain javax.naming.InitialContext JNDI initial context} setup.</li>
     *   <li>{@linkplain javax.sql.DataSource} - used as is.</li>
     *   <li>missing - default to
     *       {@value org.geotools.referencing.factory.epsg.ThreadedEpsgFactory#DATASOURCE_NAME}.</li>
     * </ul>
     * <p>
     * To set on the command line:
     * <blockquote><pre>
     * -D{@value GeoTools#EPSG_DATA_SOURCE}=<var>jndiReference</var>
     * </pre></blockquote>
     *
     * @since 2.4
     */
    public static final Key EPSG_DATA_SOURCE = new DataSourceKey();

    /**
     * The preferred datum shift method to use for
     * {@linkplain org.opengis.referencing.operation.CoordinateOperation coordinate operations}.
     * Valid values are {@code "Molodenski"}, {@code "Abridged_Molodenski"} or {@code "Geocentric"}.
     * Other values may be supplied if a {@linkplain org.opengis.referencing.operation.MathTransform
     * math transform} exists for that name, but this is not guaranteed to work.
     *
     * @see org.geotools.referencing.FactoryFinder#getCoordinateOperationFactory
     */
    public static final OptionKey DATUM_SHIFT_METHOD = new OptionKey(
            "Molodenski", "Abridged_Molodenski", "Geocentric", "*");

    /**
     * Tells if {@linkplain org.opengis.referencing.operation.CoordinateOperation coordinate
     * operations} should be allowed even when a datum shift is required while no method is
     * found applicable. It may be for example that no
     * {@linkplain org.geotools.referencing.datum.BursaWolfParameters Bursa Wolf parameters}
     * were found for a datum shift. The default value is {@link Boolean#FALSE FALSE}, which means
     * that {@linkplain org.geotools.referencing.operation.DefaultCoordinateOperationFactory
     * coordinate operation factory} throws an exception if such a case occurs. If this hint is
     * set to {@code TRUE}, then the user is strongly encouraged to check the
     * {@linkplain org.opengis.referencing.operation.CoordinateOperation#getPositionalAccuracy
     * positional accuracy} for every transformation created. If the set of positional accuracy
     * contains {@link org.geotools.metadata.iso.quality.PositionalAccuracyImpl#DATUM_SHIFT_OMITTED
     * DATUM_SHIFT_OMITTED}, this means that an "ellipsoid shift" were applied without real datum
     * shift method available, and the transformed coordinates may have one kilometer error. The
     * application should warn the user (e.g. popup a message dialog box) in such case.
     *
     * @see org.geotools.referencing.FactoryFinder#getCoordinateOperationFactory
     */
    public static final Key LENIENT_DATUM_SHIFT = new Key(Boolean.class);

    /**
     * Tells if the {@linkplain org.opengis.referencing.cs.CoordinateSystem coordinate systems}
     * created by an {@linkplain org.opengis.referencing.cs.CSAuthorityFactory authority factory}
     * should be forced to (<var>longitude</var>,<var>latitude</var>) axis order. This hint is
     * especially useful for creating
     * {@linkplan org.opengis.referencing.crs.CoordinateReferenceSystem coordinate reference system}
     * objects from <A HREF="http://www.epsg.org">EPSG</A> codes. Most
     * {@linkplan org.opengis.referencing.crs.GeographicCRS geographic CRS} defined in the EPSG
     * database use (<var>latitude</var>,<var>longitude</var>) axis order. Unfortunatly, many data
     * sources available in the world uses the opposite axis order and still claim to use a CRS
     * described by an EPSG code. This hint allows to handle such data.
     * <p>
     * This hint shall be passed to the
     * <code>{@linkplain org.geotools.referencing.FactoryFinder#getCRSAuthorityFactory
     * FactoryFinder.getCRSAuthorityFactory}(...)</code> method. Whatever this hint is supported
     * or not is authority dependent. In the default Geotools configuration, this hint is supported
     * for the {@code "EPSG"} authority.
     * <p>
     * If this hint is not provided, then the default value depends on many factors including
     * {@linkplain System#getProperties system properties} and plugins available in the classpath.
     * In Geotools implementation, the default value is usually {@link Boolean#FALSE FALSE} with
     * one exception: If the <code>{@value
     * org.geotools.referencing.factory.epsg.LongitudeFirstFactory#SYSTEM_DEFAULT_KEY}</code>
     * system property is set to {@code true}, then the default value is {@code true} at least
     * for the {@linkplain org.geotools.referencing.factory.epsg.ThreadedEpsgFactory default EPSG
     * factory}.
     * <p>
     * If both the above-cited system property and this hint are provided, then
     * this hint has precedence. This allow axis order control on a data store
     * basis, and keep the system-wide property as the default value only for
     * cases where axis order is unspecified.
     * <p>
     * To set on the command line:
     * <blockquote><pre>
     * -D{@value GeoTools#FORCE_LONGITUDE_FIRST_AXIS_ORDER}=<var>longitudeFirst</var>
     * </pre></blockquote>
     *
     * @see org.geotools.referencing.FactoryFinder#getCSFactory
     * @see org.geotools.referencing.FactoryFinder#getCRSFactory
     * @see org.geotools.referencing.factory.OrderedAxisAuthorityFactory
     * @see org.geotools.referencing.factory.epsg.LongitudeFirstFactory
     * @tutorial http://docs.codehaus.org/display/GEOTOOLS/The+axis+order+issue
     *
     * @since 2.3
     */
    public static final Key FORCE_LONGITUDE_FIRST_AXIS_ORDER = new Key(Boolean.class);

    /**
     * Tells if the {@linkplain org.opengis.referencing.cs.CoordinateSystem coordinate systems}
     * created by an {@linkplain org.opengis.referencing.cs.CSAuthorityFactory authority factory}
     * should be forced to standard
     * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis#getDirection axis directions}.
     * If {@code true}, then {@linkplain org.opengis.referencing.cs.AxisDirection#SOUTH South} axis
     * directions are forced to {@linkplain org.opengis.referencing.cs.AxisDirection#NORTH North},
     * {@linkplain org.opengis.referencing.cs.AxisDirection#WEST West} axis directions are forced to
     * {@linkplain org.opengis.referencing.cs.AxisDirection#EAST East}, <cite>etc.</cite>
     * If {@code false}, then the axis directions are left unchanged.
     * <p>
     * This hint shall be passed to the
     * <code>{@linkplain org.geotools.referencing.FactoryFinder#getCRSAuthorityFactory
     * FactoryFinder.getCRSAuthorityFactory}(...)</code>
     * method. Whatever this hint is supported or not is authority dependent.
     *
     * @see org.geotools.referencing.FactoryFinder#getCSFactory
     * @see org.geotools.referencing.FactoryFinder#getCRSFactory
     * @see org.geotools.referencing.factory.OrderedAxisAuthorityFactory
     *
     * @since 2.3
     */
    public static final Key FORCE_STANDARD_AXIS_DIRECTIONS = new Key(Boolean.class);

    /**
     * Tells if the {@linkplain org.opengis.referencing.cs.CoordinateSystem coordinate systems}
     * created by an {@linkplain org.opengis.referencing.cs.CSAuthorityFactory authority factory}
     * should be forced to standard
     * {@linkplain org.opengis.referencing.cs.CoordinateSystemAxis#getUnit axis units}.
     * If {@code true}, then all angular units are forced to degrees and linear units to meters.
     * If {@code false}, then the axis units are left unchanged.
     * <p>
     * This hint shall be passed to the
     * <code>{@linkplain org.geotools.referencing.FactoryFinder#getCRSAuthorityFactory
     * FactoryFinder.getCRSAuthorityFactory}(...)</code> method. Whatever this hint is
     * supported or not is authority dependent.
     *
     * @see org.geotools.referencing.FactoryFinder#getCSFactory
     * @see org.geotools.referencing.FactoryFinder#getCRSFactory
     * @see org.geotools.referencing.factory.OrderedAxisAuthorityFactory
     *
     * @since 2.3
     */
    public static final Key FORCE_STANDARD_AXIS_UNITS = new Key(Boolean.class);
      
    ////////////////////////////////////////////////////////////////////////
    ////////                                                        ////////
    ////////              Query hints                               ////////
    ////////                                                        ////////
    ////////////////////////////////////////////////////////////////////////
    

    /**
     * When this key is used in the user data section of a feature and the feature store
     * query capabilities reports being able to use provided feature ids the store will
     * try to use the user provided feature id during insertion, and will fail if the FID
     * cannot be parsed into a valid storage identifier
     *
     * @since 2.7
     */
    public static final Key USE_PROVIDED_FID = new Key("org.geotools.fidPolicy.UseExisting");

    ////////////////////////////////////////////////////////////////////////
    ////////                                                        ////////
    ////////                     JTS Geometries                     ////////
    ////////                                                        ////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * The {@link com.vividsolutions.jts.geom.GeometryFactory} instance to use.
     *
     * @see #GEOMETRY_FACTORY
     * @see org.geotools.geometry.jts.FactoryFinder#getGeometryFactory
     */
    public static final ClassKey JTS_GEOMETRY_FACTORY = new ClassKey(
            "com.vividsolutions.jts.geom.GeometryFactory");

    /**
     * The {@link com.vividsolutions.jts.geom.CoordinateSequenceFactory} instance to use.
     *
     * @see org.geotools.geometry.jts.FactoryFinder#getCoordinateSequenceFactory
     */
    public static final ClassKey JTS_COORDINATE_SEQUENCE_FACTORY = new ClassKey(
            "com.vividsolutions.jts.geom.CoordinateSequenceFactory");

    ////////////////////////////////////////////////////////////////////////
    ////////                                                        ////////
    ////////                        Features                        ////////
    ////////                                                        ////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * The {@link org.opengis.feature.FeatureFactory} instance to use.
     * 
     * @see CommonFactoryFinder.getFeatureFactory()
     * @since 2.5
     */
    public static ClassKey FEATURE_FACTORY = new ClassKey( "org.opengis.feature.FeatureFactory");
    
    /**
     * Indicates the features returned by the feature collections should be considered detached from
     * the datastore. If true the features can be udpated without altering the backing store.
     * <p>
     * Examples of fetures that are "attached" are features are kept in memory or features managed
     * by a transparent persistence mechanism like Hibernate.
     *
     * @since 2.4
     */
    public static final Key FEATURE_DETACHED = new Key(Boolean.class);

    /**
     * Request that the features returned by the feature collections should
     * be 2D only. Can be used to prevent the request of the third ordinate
     * when only two are going to be used.
     *
     * @since 2.4.1
     */
    public static final Key FEATURE_2D = new Key(Boolean.class);
        
    /**
     * Asks a datastore having a vector pyramid (pre-generalized geometries)
     * to return the geometry version whose points have been generalized
     * less than the spefiedi distance (further generalization might be
     * performed by the client in memory).<p>
     * The geometries returned are supposed to be topologically valid.
     */
    public static final Key GEOMETRY_DISTANCE = new Key(Double.class);
    
    /**
     * The {@link org.opengis.filter.FilterFactory} instance to use.
     *
     * @see CommonFactoryFinder#getFilterFactory
     *
     * @since 2.4
     */
    public static final ClassKey FILTER_FACTORY = new ClassKey(
            "org.opengis.filter.FilterFactory");
    
    /**
     * Resample tolerance (defaults to 0.333)
     * 
     * @since 2.7
     */
    public static final Key RESAMPLE_TOLERANCE = new Key(Double.class);

    ////////////////////////////////////////////////////////////////////////
    ////////                                                        ////////
    ////////                         Caches                         ////////
    ////////                                                        ////////
    ////////////////////////////////////////////////////////////////////////

    /**
     * Policy to use for caching referencing objects. Valid values are:
     * <p>
     * <ul>
     *   <li>{@code "weak"} for holding values through {@linkplain java.lang.ref.WeakReference
     *       weak references}. This option does not actually cache the objects since the garbage
     *       collector cleans weak references aggressively, but it allows sharing the instances
     *       already created and still in use.</li>
     *   <li>{@code "fixed") for holding a fixed number of values specified by {@link #CACHE_LIMIT}.
     *   <li>{@code "all"} for holding values through strong references.</li>
     *   <li>{@code "none"} for disabling the cache.</li>
     *   <li>{@code "soft"} for holding the value throuhg(@linkplain java.lang.ref.SoftReference 
     *       soft references}.
     * </ul>
     *
     * @since 2.5
     */
    public static final OptionKey CACHE_POLICY = new OptionKey("weak", "all", "fixed","none","default","soft");

    /**
     * The recommended maximum number of referencing objects to hold in a
     * {@linkplain org.opengis.referencing.AuthorityFactory authority factory}.
     *
     * @since 2.5
     */
    public static final IntegerKey CACHE_LIMIT = new IntegerKey(50);
    
    /**
     * Tolerance used in comparisons between floating point values. Two floating points A and B are
     * considered the same if A * (1 - tol) <= B <= A * (1 + tol).
     * The default value is 0, meaning the two doubles have to be exactly the same (a bit to bit
     * comparison will be performed).
     * 
     * @since 2.6
     */
    public static final DoubleKey COMPARISON_TOLERANCE = new DoubleKey(0.0);

    /**
     * Constructs an initially empty set of hints.
     *
     * @since 2.5
     */
    public Hints() {
        super(null);
    }

    /**
     * Constructs a new object with the specified key/value pair.
     *
     * @param key   The key of the particular hint property.
     * @param value The value of the hint property specified with {@code key}.
     */
    public Hints(final RenderingHints.Key key, final Object value) {
        super(null); // Don't use 'super(key,value)' because it doesn't check validity.
        super.put(key, value);
    }

    /**
     * Constructs a new object with keys and values initialized from the
     * specified map (which may be null).
     *
     * @param hints A map of key/value pairs to initialize the hints, or
     *              {@code null} if the object should be empty.
     */
    public Hints(final Map<? extends RenderingHints.Key, ?> hints) {
        super(stripNonKeys(hints));
    }

    /**
     * Constructs a new object with keys and values initialized from the
     * specified hints (which may be null).
     *
     * @param hints A map of key/value pairs to initialize the hints, or
     *              {@code null} if the object should be empty.
     *
     * @since 2.5
     */
    public Hints(final RenderingHints hints) {
        super(stripNonKeys(hints));
    }

    /**
     * Returns a map with the same hints than the specified one, minus every (key,value)
     * pairs where the key is not an instance of {@link RenderingHints.Key}. If the given
     * map contains only valid keys, then it is returned unchanged.
     *
     * @param  hints The map of hints to filter.
     * @return A map with filtered hints.
     */
    static Map<RenderingHints.Key, Object> stripNonKeys(final Map<?,?> hints) {
        if (hints == null) {
            return null;
        }
        /*
         * We cheat in the next line since the map may contains illegal key. However when this
         * method will finish, we garantee that it will contains only RenderingHints.Key keys,
         * provided there is no concurrent changes in an other thread.
         */
        @SuppressWarnings("unchecked")
        Map<RenderingHints.Key, Object> filtered = (Map) hints;
        for (final Iterator it=hints.keySet().iterator(); it.hasNext();) {
            final Object key = it.next();
            if (!(key instanceof RenderingHints.Key)) {
                if (filtered == hints) {
                    // Copies the map only if needed.
                    filtered = new HashMap<RenderingHints.Key, Object>(filtered);
                }
                filtered.remove(key);
            }
        }
        return filtered;
    }

    /**
     * Returns a new map of hints with the same content than this map.
     *
     * @since 2.5
     */
    @Override
    public Hints clone() {
        return (Hints) super.clone();
    }

    /**
     * Invokes {@link GeoTools#scanSystemProperties} when first needed. The caller is
     * responsible for invoking {@link GeoTools#fireConfigurationChanged} outside the
     * synchronized block if this method returns {@code true}.
     *
     * @return {@code true} if at least one hint changed as a result of this scan,
     *         or {@code false} otherwise.
     */
    private static boolean ensureSystemDefaultLoaded() {
        assert Thread.holdsLock(GLOBAL);
        if (needScan) {
            needScan = false;
            return GeoTools.scanForSystemHints(GLOBAL);
        } else {
            return false;
        }
    }

    /**
     * Returns a copy of the system hints. This is for
     * {@link GeoTools#getDefaultHints} implementation only.
     */
    static Hints getDefaults(final boolean strict) {
        final boolean changed;
        final Hints hints;
        synchronized (GLOBAL) {
            changed = ensureSystemDefaultLoaded();
            if (strict) {
                hints = new StrictHints(GLOBAL);
            } else {
                hints = new Hints(GLOBAL);
            }
        }
        if (changed) {
            GeoTools.fireConfigurationChanged();
        }
        return hints;
    }

    /**
     * Returns the hint {@linkplain GeoTools#getDefaultHints default value}
     * for the specified key.
     *
     * @param  key The hints key.
     * @return The value for the specified key, or {@code null}
     *         if the key did not have a mapping.
     *
     * @since 2.4
     */
    public static Object getSystemDefault(final RenderingHints.Key key) {
        final boolean changed;
        final Object value;
        synchronized (GLOBAL) {
            changed = ensureSystemDefaultLoaded();
            value = GLOBAL.get(key);
        }
        if (changed) {
            GeoTools.fireConfigurationChanged();
        }
        return value;
    }

    /**
     * Adds a hint value to the set of {@linkplain GeoTools#getDefaultHints default hints}.
     * Default hints can be added by call to this {@code putDefaultHint} method, to the
     * {@link GeoTools#init} method or by {@linkplain System#getProperties system properties}
     * with keys defined by the {@link String} constants in the {@link GeoTools} class.
     *
     * @param key   The hint key.
     * @param value The hint value.
     * @return The previous value of the specified key, or {@code null} if none.
     * @throws IllegalArgumentException If {@link Hints.Key#isCompatibleValue()}
     *         returns {@code false} for the specified value.
     *
     * @since 2.4
     */
    public static Object putSystemDefault(final RenderingHints.Key key, final Object value) {
        final boolean changed;
        final Object old;
        synchronized (GLOBAL) {
            changed = ensureSystemDefaultLoaded();
            old = GLOBAL.put(key, value);
        }
        if (changed || !Utilities.equals(value, old)) {
            GeoTools.fireConfigurationChanged();
        }
        return old;
    }

    /**
     * Removes the specified hints from the set of
     * {@linkplain GeoTools#getDefaultHints default hints}.
     *
     * @param  key The hints key that needs to be removed.
     * @return The value to which the key had previously been mapped,
     *         or {@code null} if the key did not have a mapping.
     *
     * @since 2.4
     */
    public static Object removeSystemDefault(final RenderingHints.Key key) {
        final boolean changed;
        final Object old;
        synchronized (GLOBAL) {
            changed = ensureSystemDefaultLoaded();
            old = GLOBAL.remove(key);
        }
        if (changed || old != null) {
            GeoTools.fireConfigurationChanged();
        }
        return old;
    }

    /**
     * Returns a string representation of the hints. This method formats the set of hints
     * as a tree. If some system-wide {@linkplain GeoTools#getDefaultHints default hints}
     * exist, they are formatted after those hints for completeness.
     *
     * @since 2.4
     */
    @Override
    public String toString() {
        final String lineSeparator = System.getProperty("line.separator", "\n");
        final StringBuilder buffer = new StringBuilder("Hints:"); // TODO: localize
        buffer.append(lineSeparator).append(AbstractFactory.toString(this));
        Map<?,?> extra = null;
        final boolean changed;
        synchronized (GLOBAL) {
            changed = ensureSystemDefaultLoaded();
            if (!GLOBAL.isEmpty()) {
                extra = new HashMap<Object,Object>(GLOBAL);
            }
        }
        if (changed) {
            GeoTools.fireConfigurationChanged();
        }
        if (extra != null) {
            extra.keySet().removeAll(keySet());
            if (!extra.isEmpty()) {
                buffer.append("System defaults:")  // TODO: localize
                      .append(lineSeparator).append(AbstractFactory.toString(extra));
            }
        }
        return buffer.toString();
    }

    /**
     * Tries to find the name of the given key, using reflection.
     */
    static String nameOf(final RenderingHints.Key key) {
        if (key instanceof Key) {
            return key.toString();
        }
        int t = 0;
        while (true) {
            final Class<?> type;
            switch (t++) {
                case 0: {
                    type = RenderingHints.class;
                    break;
                }
                case 1: {
                    try {
                        type = Class.forName("javax.media.jai.JAI");
                        break;
                    } catch (ClassNotFoundException e) {
                        continue;
                    } catch (NoClassDefFoundError e) {
                        // May occurs because of indirect JAI dependencies.
                        continue;
                    }
                }
                default: {
                    return key.toString();
                }
            }
            final String name = nameOf(type, key);
            if (name != null) {
                return name;
            }
        }
    }

    /**
     * If the given key is declared in the given class, returns its name.
     * Otherwise returns {@code null}.
     */
    private static String nameOf(final Class<?> type, final RenderingHints.Key key) {
        final Field[] fields = type.getFields();
        for (int i=0; i<fields.length; i++) {
            final Field f = fields[i];
            if (Modifier.isStatic(f.getModifiers())) {
                final Object v;
                try {
                    v = f.get(null);
                } catch (IllegalAccessException e) {
                    continue;
                }
                if (v == key) {
                    return f.getName();
                }
            }
        }
        return null;
    }

    /**
     * The type for keys used to control various aspects of the factory
     * creation. Factory creation impacts rendering (which is why extending
     * {@linkplain java.awt.RenderingHints.Key rendering key} is not a complete
     * non-sense), but may impact other aspects of an application as well.
     *
     * @since 2.1
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Martin Desruisseaux
     */
    public static class Key extends RenderingHints.Key {
        /**
         * The number of key created up to date.
         */
        private static int count;

        /**
         * The class name for {@link #valueClass}.
         */
        private final String className;

        /**
         * Base class of all values for this key. Will be created from {@link #className} only when
         * first required, in order to avoid too early class loading. This is significant for the
         * {@link #JAI_INSTANCE} key for example, in order to avoid JAI dependencies in applications
         * that do not need it.
         */
        private transient Class<?> valueClass;

        /**
         * Constructs a new key for values of the given class.
         *
         * @param classe The base class for all valid values.
         */
        public Key(final Class<?> classe) {
            this(classe.getName());
            valueClass = classe;
        }

        /**
         * Constructs a new key for values of the given class. The class is
         * specified by name instead of a {@link Class} object. This allows to
         * defer class loading until needed.
         *
         * @param className Name of base class for all valid values.
         */
        Key(final String className) {
            super(count());
            this.className = className;
        }

        /**
         * Workaround for RFE #4093999 ("Relax constraint on placement of this()/super()
         * call in constructors"): {@code count++} need to be executed in a synchronized
         * block since it is not an atomic operation.
         */
        private static synchronized int count() {
            return count++;
        }

        /**
         * Returns the expected class for values stored under this key.
         *
         * @return The class of values stored under this key.
         */
        public Class<?> getValueClass() {
            if (valueClass == null) {
                try {
                    valueClass = Class.forName(className);
                } catch (ClassNotFoundException exception) {
                    Logging.unexpectedException(Key.class, "getValueClass", exception);
                    valueClass = Object.class;
                }
            }
            return valueClass;
        }

        /**
         * Returns {@code true} if the specified object is a valid value for
         * this key. The default implementation checks if the specified value
         * {@linkplain Class#isInstance is an instance} of the {@linkplain
         * #getValueClass value class}.
         * <p>
         * Note that many hint keys defined in the {@link Hints} class relax this rule and accept
         * {@link Class} object assignable to the expected {@linkplain #getValueClass value class}
         * as well.
         *
         * @param value
         *            The object to test for validity.
         * @return {@code true} if the value is valid; {@code false} otherwise.
         *
         * @see Hints.ClassKey#isCompatibleValue
         * @see Hints.FileKey#isCompatibleValue
         * @see Hints.IntegerKey#isCompatibleValue
         * @see Hints.OptionKey#isCompatibleValue
         */
        public boolean isCompatibleValue(final Object value) {
            return getValueClass().isInstance(value);
        }

        /**
         * Returns a string representation of this key. The string
         * representation is mostly for debugging purpose. The default
         * implementation tries to infer the key name using reflection.
         */
        @Override
        public String toString() {
            int t = 0;
            while (true) {
                final Class<?> type;
                switch (t++) {
                    case 0:  type = Hints.class;      break;
                    case 1:  type = getValueClass();  break;
                    default: return super.toString();
                }
                final String name = nameOf(type, this);
                if (name != null) {
                    return name;
                }
            }
        }
    }

    /**
     * A key for value that may be specified either as instance of {@code T}, or as
     * {@code Class<T>}.
     *
     * @since 2.4
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Martin Desruisseaux
     */
    public static final class ClassKey extends Key {
        
        /**
         * Constructs a new key for values of the given class. The class is
         * specified by name instead of a {@link Class} object. This allows to
         * defer class loading until needed.
         *
         * @param className Name of base class for all valid values.
         */
        ClassKey(final String className) {
            super(className);
        }

        /**
         * Returns {@code true} if the specified object is a valid value for
         * this key. This method checks if the specified value is non-null and
         * is one of the following:
         * <p>
         * <ul>
         *   <li>An instance of the {@linkplain #getValueClass expected value class}.</li>
         *   <li>A {@link Class} assignable to the expected value class.</li>
         *   <li>An array of {@code Class} objects assignable to the expected value class.</li>
         * </ul>
         */
        @Override
        public boolean isCompatibleValue(final Object value) {
            if (value == null) {
                return false;
            }
            /*
             * If the value is an array of classes, invokes this method recursively
             * in order to check the validity of each elements in the array.
             */
            if (value instanceof Class<?>[]) {
                final Class<?>[] types = (Class<?>[]) value;
                for (int i=0; i<types.length; i++) {
                    if (!isCompatibleValue(types[i])) {
                        return false;
                    }
                }
                return types.length != 0;
            }
            /*
             * If the value is a class, checks if it is assignable to the expected value class.
             * As a special case, if the value is not assignable but is an abstract class while
             * we expected an interface, we will accept this class anyway because the some sub-
             * classes may implement the interface (we dont't really know). For example the
             * AbstractAuthorityFactory class doesn't implements the CRSAuthorityFactory interface,
             * but sub-classe of it do. We make this relaxation in order to preserve compatibility,
             * but maybe we will make the check stricter in the future.
             */
            if (value instanceof Class<?>) {
                final Class<?> type = (Class<?>) value;
                final Class<?> expected = getValueClass();
                if (expected.isAssignableFrom(type)) {
                    return true;
                }
                if (expected.isInterface() && !type.isInterface()) {
                    final int modifiers = type.getModifiers();
                    if (Modifier.isAbstract(modifiers) && !Modifier.isFinal(modifiers)) {
                        return true;
                    }
                }
                return false;
            }
            return super.isCompatibleValue(value);
        }
    }

    /**
     * Key for hints to be specified as a {@link File}.
     * The file may also be specified as a {@link String} object.
     *
     * @since 2.4
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Jody Garnett
     * @author Martin Desruisseaux
     */
    public static final class FileKey extends Key {
        /**
         * {@code true} if write operations need to be allowed.
         */
        private final boolean writable;

        /**
         * Creates a new key for {@link File} value.
         *
         * @param writable {@code true} if write operations need to be allowed.
         */
        public FileKey(final boolean writable) {
            super(File.class);
            this.writable = writable;
        }

        /**
         * Returns {@code true} if the specified object is a valid file or directory.
         */
        @Override
        public boolean isCompatibleValue(final Object value) {
            final File file;
            if (value instanceof File) {
                file = (File) value;
            } else if (value instanceof String) {
                file = new File((String) value);
            } else {
                return false;
            }
            if (file.exists()) {
                return !writable || file.canWrite();
            }
            final File parent = file.getParentFile();
            return parent!=null && parent.canWrite();
        }
    }

    /**
     * A hint used to capture a configuration setting as an integer.
     * A default value is provided and may be checked with {@link #getDefault()}.
     *
     * @since 2.4
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Jody Garnett
     */
    public static final class IntegerKey extends Key {

        /**
         * Creates a new key with the specified default value.
         *
         * @param number The default value.
         */
        public IntegerKey(final int number) {
            super(Integer.class);
        }

        /**
         * Returns {@code true} if the specified object is a valid integer.
         */
        @Override
        public boolean isCompatibleValue(final Object value) {
            if (value instanceof Short || value instanceof Integer) {
                return true;
            }
            if (value instanceof String || value instanceof InternationalString) {
                try {
                    Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    Logging.getLogger(IntegerKey.class).finer(e.toString());
                }
            }
            return false;
        }
    }
    
    /**
     * A hint used to capture a configuration setting as double.
     * A default value is provided and may be checked with {@link #getDefault()}.
     *
     * @since 2.6
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Jody Garnett
     */
    public static final class DoubleKey extends Key {
        /**
         * The default value.
         */
        private final double number;

        /**
         * Creates a new key with the specified default value.
         *
         * @param number The default value.
         */
        public DoubleKey(final double number) {
            super(Integer.class);
            this.number = number;
        }

        /**
         * Returns the default value.
         *
         * @return The default value.
         */
        public double getDefault(){
            return number;
        }

        /**
         * Returns {@code true} if the specified object is a valid integer.
         */
        @Override
        public boolean isCompatibleValue(final Object value) {
            if (value instanceof Float || value instanceof Double) {
                return true;
            }
            if (value instanceof String || value instanceof InternationalString) {
                try {
                    Double.parseDouble(value.toString());
                } catch (NumberFormatException e) {
                    Logging.getLogger(DoubleKey.class).finer(e.toString());
                }
            }
            return false;
        }
    }

    /**
     * Key that allows the choice of several options.
     * You can use {@code "*"} as a wild card to indicate that undocumented options
     * may be supported (but there is no assurances - {@link Hints#DATUM_SHIFT_METHOD}).
     *
     * @since 2.4
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Jody Garnett
     */
    public static final class OptionKey extends Key {
        /**
         * The set of options allowed.
         */
        private final Set<String> options;

        /**
         * {@code true} if the {@code "*"} wildcard was given in the set of options.
         */
        private final boolean wildcard;

        /**
         * Creates a new key for a configuration option.
         *
         * @param alternatives The available options.
         */
        public OptionKey(final String... alternatives) {
            super(String.class);
            final Set<String> options = new TreeSet<String>(Arrays.asList(alternatives));
            this.wildcard = options.remove("*");
            this.options  = Collections.unmodifiableSet(options);
        }

        /**
         * Returns {@code true} if the specified object is one of the valid options. If the
         * options specified at construction time contains the {@code "*"} wildcard, then
         * this method returns {@code true} for every {@link String} object.
         */
        @Override
        public boolean isCompatibleValue(final Object value) {
            return wildcard ? (value instanceof String) : options.contains(value);
        }
    }

    /**
     * Key for hints to be specified as a {@link javax.sql.DataSource}.
     * The file may also be specified as a {@link String} or {@link Name} object.
     * <p>
     * Different JNDI implementations build up their name differently (so we may need
     * to look for "jdbc:EPSG" in JBoss and "jdbc/EPSG" in Websphere. The
     * InitialContext.combineNames( String, String ) should be used to put together
     * your nam
     *
     * @since 2.4
     * @source $URL: http://svn.osgeo.org/geotools/branches/2.7.x/modules/library/metadata/src/main/java/org/geotools/factory/Hints.java $
     * @version $Id: Hints.java 38414 2011-12-13 09:39:22Z aaime $
     * @author Martin Desruisseaux
     */
    static final class DataSourceKey extends Key {
        /**
         * Creates a new key for {@link javax.sql.DataSource} value.
         */
        public DataSourceKey() {
            super(DataSource.class);
        }

        /**
         * Returns {@code true} if the specified object is a data source or data source name.
         */
        @Override
        public boolean isCompatibleValue(final Object value) {
            return (value instanceof DataSource) || (value instanceof String) || (value instanceof Name);
        }
    }
}
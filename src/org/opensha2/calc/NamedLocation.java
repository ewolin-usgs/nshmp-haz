package org.opensha2.calc;

import org.opensha2.geo.Location;
import org.opensha2.util.Named;

/**
 * Marker interface for {@code enum}s of {@link Location}s. This interface is
 * distinct from {@link Named} due to shadowing of {@link Enum#name()}.
 * Typically, implementating enum types return a human-readable label via
 * {@code toString()} and return the value typically returned by
 * {@link Enum#name()} via {@link #id()} .
 *
 * @author Peter Powers
 */
public interface NamedLocation {

	/**
	 * Return the location.
	 */
	Location location();

	/**
	 * Return a unique id for the location. This is typically the value returned
	 * by {@link Enum#name()}.
	 */
	String id();

}

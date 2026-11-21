package com.holub.life;

import com.holub.life.LifePattern.Position;

import java.util.*;

/**
 * Stores built-in patterns as private prototypes in menu order. Each selection
 * receives its own {@link LifePattern} copy, so rotating or editing a selection
 * leaves the original shape available the next time it is selected.
 */
final class PatternCatalog
{
	private final Map<String, LifePattern> prototypes = new LinkedHashMap<>();

	/** Registers the built-in prototypes in the order shown in the preset menu. */
	PatternCatalog()
	{	prototypes.put( "Glider", new LifePattern( 3, 3, List.of(
				new Position( 1, 0 ),
				new Position( 2, 1 ),
				new Position( 0, 2 ),
			    new Position( 1, 2 ),
				new Position( 2, 2 ) ) ) ); // A three-row Glider.

		// The Blinker in its initial horizontal orientation.
		prototypes.put( "Blinker", new LifePattern( 3, 1, List.of(
				new Position( 0, 0 ),
				new Position( 1, 0 ),
				new Position( 2, 0 ) ) ) );

		// The Block is a still life with four live cells in a square.
		prototypes.put( "Block", new LifePattern( 2, 2, List.of(
					new Position( 0, 0 ), new Position( 1, 0 ),
					new Position( 0, 1 ), new Position( 1, 1 ) ) ) );
	}

	/** @return an unmodifiable set of preset names in registration order */
	Set<String> names()
	{	return Collections.unmodifiableSet( prototypes.keySet() ); // Preserve the map's insertion order.
	}

	/**
	 * Creates a working copy of the original pattern registered under the given name.
	 *
	 * @param name exact name as returned by {@link #names()}
	 * @return an independent copy in the original orientation
	 * @throws IllegalArgumentException if no preset is registered under {@code name}
	 */
	LifePattern copyOf( String name )
	{	LifePattern prototype = prototypes.get( name );
		// A missing name cannot produce a usable selection.
		if( prototype == null )
			throw new IllegalArgumentException( "Unknown preset: " + name );

		// Keep the registered prototype private.
		return prototype.copy();
	}
}

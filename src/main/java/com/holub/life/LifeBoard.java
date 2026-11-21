package com.holub.life;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/** Runs each generation on the existing composite board. */
final class LifeBoard
{
	private final Neighborhood root;
	private BoundaryPolicy boundaryPolicy = BoundaryPolicy.FIXED;
	private boolean recalculateAll;

	LifeBoard( Neighborhood root )
	{	this.root = Objects.requireNonNull( root );
	}

	BoundaryPolicy boundaryPolicy()
	{	return boundaryPolicy;
	}

	void setBoundaryPolicy( BoundaryPolicy policy )
	{	if( boundaryPolicy != Objects.requireNonNull( policy ) )
		{
			boundaryPolicy = policy;
			recalculateAll = true;
		}
	}

	boolean step()
	{	Cell outside = boundaryPolicy.outsideNeighbor( root );
		boolean changed = root.figureNextState( outside, outside, outside, outside,
				outside, outside, outside, outside, recalculateAll );
		recalculateAll = false;
		if( !changed )
		{
			// In wrap mode the root is its own neighbor. Retire its old edge flags.
			root.clearActiveEdges();
			return false;
		}
		return root.transition();
	}

	void load( InputStream input ) throws IOException
	{	Storable memento = root.createMemento();
		memento.load( input );
		restore( memento );
	}

	/** Rebuild active blocks on the next step under the selected boundary policy. */
	void restore( Storable memento )
	{	root.clear();
		root.transfer( memento, new Point( 0, 0 ), Cell.LOAD );
		recalculateAll = true;
	}

	void store( OutputStream output ) throws IOException
	{	root.createMemento().flush( output );
	}
}

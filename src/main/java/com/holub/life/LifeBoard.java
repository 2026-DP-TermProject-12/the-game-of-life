package com.holub.life;

import java.awt.Point;
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

	void load( BoardSnapshot snapshot )
	{	root.restoreSnapshot( snapshot );
		recalculateAll = true;
	}

	BoardSnapshot snapshot()
	{	return root.snapshot();
	}

	/** Rebuild active blocks on the next step under the selected boundary policy. */
	void restore( Cell.Memento memento )
	{	root.clear();
		root.transfer( memento, new Point( 0, 0 ), Cell.LOAD );
		recalculateAll = true;
	}
}

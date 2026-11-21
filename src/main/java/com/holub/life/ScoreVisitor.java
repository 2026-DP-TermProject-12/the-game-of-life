package com.holub.life;

import java.util.BitSet;

/**
 * Traverses the Cell hierarchy to collect live cell count
 * and an exact bit-pattern snapshot of the current board layout.
 */
public class ScoreVisitor implements CellVisitor
{
	private int          liveCount  = 0;
	private int          cellIndex  = 0;
	private final BitSet boardState = new BitSet();

	public void visit( Resident cell )
	{	if( cell.isAlive() )
		{	liveCount++;
			boardState.set( cellIndex );
		}
		cellIndex++;
	}

	public void visit( Neighborhood grid )
	{	grid.visitChildren( this );
	}

	public int getLiveCount()
	{	return liveCount;
	}

	public BitSet getBoardState()
	{	return (BitSet) boardState.clone();
	}
}
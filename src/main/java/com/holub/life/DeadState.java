package com.holub.life;

/**
 * Concrete state representing a dead cell.
 */
public class DeadState implements CellState
{
	public static final CellState INSTANCE = new DeadState();

	private DeadState()
	{	// Singleton instance to prevent unnecessary object allocation
	}

	public CellState computeNextState( int liveNeighbors )
	{	if( liveNeighbors == 3 )
		{	return AliveState.INSTANCE;
		}
		return DeadState.INSTANCE;
	}

	public boolean isAlive()
	{	return false;
	}

	public CellState toggle()
	{	return AliveState.INSTANCE;
	}
}
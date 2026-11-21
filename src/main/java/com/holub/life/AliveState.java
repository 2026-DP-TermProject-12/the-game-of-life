package com.holub.life;

/**
 * Concrete state representing a living cell.
 */
public class AliveState implements CellState
{
	public static final CellState INSTANCE = new AliveState();

	private AliveState()
	{	// Singleton instance to prevent unnecessary object allocation
	}

	public CellState computeNextState( int liveNeighbors )
	{	if( liveNeighbors == 2 || liveNeighbors == 3 )
		{	return AliveState.INSTANCE;
		}
		return DeadState.INSTANCE;
	}

	public boolean isAlive()
	{	return true;
	}

	public CellState toggle()
	{	return DeadState.INSTANCE;
	}
}
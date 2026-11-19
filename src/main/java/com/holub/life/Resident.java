package com.holub.life;

import java.awt.*;
import com.holub.ui.Colors;

/**
 * Represents a single cell in the Game of Life.
 * Delegates next-state evaluation and toggling behavior to CellState.
 */
public final class Resident implements Cell
{
	private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
	private static final Color LIVE_COLOR   = Color.RED;
	private static final Color DEAD_COLOR   = Colors.LIGHT_YELLOW;

	private CellState currentState = DeadState.INSTANCE;
	private CellState nextState    = DeadState.INSTANCE;

	/**
	 * Counts living neighbors and delegates next-state decision
	 * to the current state object.
	 */
	public boolean figureNextState(
		Cell north,     Cell south,
		Cell east,      Cell west,
		Cell northeast, Cell northwest,
		Cell southeast, Cell southwest )
	{	int liveNeighbors = 0;

		if( north.isAlive()     ) liveNeighbors++;
		if( south.isAlive()     ) liveNeighbors++;
		if( east.isAlive()      ) liveNeighbors++;
		if( west.isAlive()      ) liveNeighbors++;
		if( northeast.isAlive() ) liveNeighbors++;
		if( northwest.isAlive() ) liveNeighbors++;
		if( southeast.isAlive() ) liveNeighbors++;
		if( southwest.isAlive() ) liveNeighbors++;

		nextState = currentState.computeNextState( liveNeighbors );
		return !isStable();
	}

	/**
	 * Returns true if the cell state will remain unchanged.
	 */
	private boolean isStable()
	{	return currentState == nextState;
	}

	/**
	 * Commits the precomputed next state as the current state.
	 */
	public boolean transition()
	{	boolean changed = !isStable();
		currentState = nextState;
		return changed;
	}

	/**
	 * Checks whether this cell is currently alive.
	 */
	public boolean isAlive()
	{	return currentState.isAlive();
	}

	/**
	 * ResetStates the cell to the dead state.
	 */
	public void clear()
	{	currentState = DeadState.INSTANCE;
		nextState    = DeadState.INSTANCE;
	}

	/**
	 * Returns the current state object of this cell.
	 */
	public CellState getState()
	{	return currentState;
	}
	/**
	 * Toggles the cell state between alive and dead.
	 */
	public void toggleState()
	{	currentState = currentState.toggle();
	}

	/**
	 * Handles user click interaction by toggling the cell state.
	 */
	public void userClicked( Point here, Rectangle surface )
	{	toggleState();
	}

	/**
	 * Renders this cell based on its current state.
	 */
	public void redraw( Graphics g, Rectangle here, boolean drawAll )
	{	g = g.create();
		g.setColor( isAlive() ? LIVE_COLOR : DEAD_COLOR );
		g.fillRect( here.x + 1, here.y + 1, here.width - 1, here.height - 1 );

		g.setColor( BORDER_COLOR );
		g.drawLine( here.x, here.y, here.x, here.y + here.height );
		g.drawLine( here.x, here.y, here.x + here.width, here.y );
		g.dispose();
	}

	/**
	 * Creates a new Resident instance.
	 */
	public Cell create()
	{	return new Resident();
	}

	/**
	 * Returns the width of a single cell in grid units.
	 */
	public int widthInCells()
	{	return 1;
	}

	public Cell edge( int row, int column )
	{	return this;
	}

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL;
	}

	/**
	 * Transfers cell state to or from a Storable memento.
	 */
	public boolean transfer( Storable blob, Point upperLeft, boolean doLoad )
	{	Neighborhood.Memento memento = (Neighborhood.Memento) blob;
		if( doLoad )
		{	boolean loadedAlive = memento.isAlive( upperLeft );
			currentState = loadedAlive ? AliveState.INSTANCE : DeadState.INSTANCE;
			nextState    = currentState;
			return isAlive();
		}
		if( isAlive() )
		{	memento.markAsAlive( upperLeft );
		}
		return false;
	}

	public Storable createMemento()
	{	throw new UnsupportedOperationException(
			"Cannot create memento of individual Resident" );
	}
}
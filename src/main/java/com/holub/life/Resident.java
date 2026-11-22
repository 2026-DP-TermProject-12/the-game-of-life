package com.holub.life;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

/**
 * Represents a single cell in the Game of Life.
 * Delegates state behavior to CellState and rendering to ResidentView.
 */
public final class Resident implements Cell
{
	private CellState currentState = DeadState.INSTANCE;
	private CellState nextState    = DeadState.INSTANCE;

	private final RendererSelection renderers;
	private final ResidentView view;

	/** Creates a cell with the standard rendering strategy. */
	public Resident()
	{	this( new RendererSelection( DefaultCellRenderer.instance() ) );
	}

	/** Creates a cell using the board's shared strategy selection. */
	Resident( RendererSelection renderers )
	{	this.renderers = renderers;
		view = new ResidentView( renderers );
	}

	/**
	 * Counts living neighbors and delegates next-state decision
	 * to the current state object.
	 */
	public boolean figureNextState(
		Cell north,     Cell south,
		Cell east,      Cell west,
		Cell northeast, Cell northwest,
		Cell southeast, Cell southwest )
	{	verify( north,     "north" );
        verify( south,     "south" );
        verify( east,      "east" );
        verify( west,      "west" );
        verify( northeast, "northeast" );
        verify( northwest, "northwest" );
        verify( southeast, "southeast" );
        verify( southwest, "southwest" );

        int liveNeighbors = 0;

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
     * Verifies that a neighbor is an atomic cell or the boundary dummy.
     */
    private void verify( Cell cell, String direction )
    {	assert (cell instanceof Resident) || (cell == Cell.DUMMY)
            : "incorrect type for " + direction + ": "
              + cell.getClass().getName();
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
	 * Resets the current and next states to dead.
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
	 * Delegates rendering using the current cell state.
	 */
	public void redraw( Graphics g, Rectangle here, boolean drawAll )
	{	view.redraw( g, here, isAlive(), drawAll );
	}

	/**
	 * Creates a new Resident instance.
	 */
	public Cell create()
	{	return new Resident( renderers );
	}

	/**
	 * Returns the width of a single cell in grid units.
	 */
	public int widthInCells()
	{	return 1;
	}

    public Cell edge( int row, int column )
    {	assert row == 0 && column == 0;
        return this;
    }

	public Direction isDisruptiveTo()
	{	return isStable() ? Direction.NONE : Direction.ALL;
	}

    /**
     * Transfers cell state to or from a Storable memento.
     */
    public boolean transfer( Storable blob, Point upperLeft, boolean doLoad )
    {	Cell.Memento memento = (Cell.Memento) blob;

        if( doLoad )
        {	boolean loadedAlive = memento.isAlive( upperLeft );
            currentState = loadedAlive ? AliveState.INSTANCE : DeadState.INSTANCE;
            nextState    = currentState;
            return isAlive();
        }

        if( isAlive() )
            memento.markAsAlive( upperLeft );

        return false;
    }

	public Storable createMemento()
	{	throw new UnsupportedOperationException(
			"Cannot create memento of individual Resident" );
	}

    /**
     * Accepts a visitor using double dispatch.
     */
    public void accept( CellVisitor visitor )
    {	visitor.visit( this );
    }
}

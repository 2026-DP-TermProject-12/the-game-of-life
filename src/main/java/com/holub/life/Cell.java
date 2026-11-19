package com.holub.life;

import java.awt.*;

/**
 * Component interface for the Game of Life composite grid.
 */
public interface Cell
{
	boolean STORE = false;
	boolean LOAD  = true;

	boolean figureNextState(
		Cell north,     Cell south,
		Cell east,      Cell west,
		Cell northeast, Cell northwest,
		Cell southeast, Cell southwest );

	Cell edge( int row, int column );
	boolean transition();
	void redraw( Graphics g, Rectangle here, boolean drawAll );
	void userClicked( Point here, Rectangle surface );
	boolean isAlive();
	int widthInCells();
	Cell create();
	Direction isDisruptiveTo();
	void clear();
	boolean transfer( Storable memento, Point upperLeftCorner, boolean doLoad );
	Storable createMemento();

	/**
	 * Accepts a visitor to perform operations across the Cell hierarchy.
	 */
	void accept( CellVisitor visitor );

	/**
	 * Memento interface for storing and loading cell states.
	 */
	public interface Memento extends Storable
	{	void    markAsAlive( Point location );
		boolean isAlive    ( Point location );
	}

	/**
	 * Null Object representing an always-dead boundary cell.
	 */
	public static final Cell DUMMY = new Cell()
	{
		public boolean figureNextState(
			Cell n, Cell s, Cell e, Cell w,
			Cell ne, Cell nw, Cell se, Cell sw )        { return false;          }
		public Cell edge( int r, int c )                { return this;           }
		public boolean transition()                     { return false;          }
		public void redraw( Graphics g, Rectangle h, boolean a ){}
		public void userClicked( Point h, Rectangle s ) {}
		public boolean isAlive()                        { return false;          }
		public int widthInCells()                       { return 0;              }
		public Cell create()                            { return this;           }
		public Direction isDisruptiveTo()               { return Direction.NONE; }
		public void clear()                             {}
		public boolean transfer( Storable m, Point u, boolean l ){ return false; }
		public Storable createMemento()
		{	throw new UnsupportedOperationException(
				"Cannot create memento of dummy block" );
		}
		public void accept( CellVisitor visitor )       {}
	};
}
package com.holub.life;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import javax.swing.*;

import com.holub.io.Files;
import com.holub.life.Cell;
import com.holub.ui.MenuSite;
import com.holub.ui.Colors;
import com.holub.asynch.ConditionVariable;

import com.holub.life.Clock;
import com.holub.life.Direction;
import com.holub.life.Storable;

/***
 * A group of {@link Cell} objects. Cells are grouped into neighborhoods
 * to make board updates more efficient. When a neighborhood is
 * quiescent (none of the Cells it contains are active), then it
 * ignores any requests to update itself.
 *
 * @include /etc/license.txt
 */
public final class Neighborhood implements Cell
{
	private static final ConditionVariable readingPermitted =
											new ConditionVariable(true);

	private boolean amActive = false;
	private final Cell[][] grid;
	private final int      gridSize;

	public Neighborhood(int gridSize, Cell prototype)
	{
		this.gridSize = gridSize;
		this.grid = new Cell[gridSize][gridSize];

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column] = prototype.create();
	}

	public Cell create()
	{	return new Neighborhood(gridSize, grid[0][0]);
	}

	private boolean oneLastRefreshRequired = false;

	public  Direction isDisruptiveTo(){ return activeEdges; }
	private Direction activeEdges = new Direction( Direction.NONE );

	public boolean figureNextState( Cell north,     Cell south,
									Cell east,      Cell west,
									Cell northeast, Cell northwest,
									Cell southeast, Cell southwest )
	{
		boolean nothingHappened = true;

		if(		amActive
			||	north    .isDisruptiveTo().the( Direction.SOUTH     )
			||	south    .isDisruptiveTo().the( Direction.NORTH     )
			||	east     .isDisruptiveTo().the( Direction.WEST      )
			||	west     .isDisruptiveTo().the( Direction.EAST      )
			||	northeast.isDisruptiveTo().the( Direction.SOUTHWEST )
			||	northwest.isDisruptiveTo().the( Direction.SOUTHEAST )
			||	southeast.isDisruptiveTo().the( Direction.NORTHWEST )
			||	southwest.isDisruptiveTo().the( Direction.NORTHEAST )
		)
		{
			Cell	northCell,     southCell,
					eastCell,      westCell,
					northeastCell, northwestCell,
					southeastCell, southwestCell;

			for( int row = 0; row < gridSize; ++row )
			{	for( int column = 0; column < gridSize; ++column )
				{
					if( row == 0 )
					{	northwestCell = (column == 0)
							? northwest.edge(gridSize - 1, gridSize - 1)
							: north.edge    (gridSize - 1, column - 1)
							;

						northCell = north.edge(gridSize - 1, column);

						northeastCell = (column == gridSize - 1)
							? northeast.edge(gridSize - 1, 0)
							: north.edge    (gridSize - 1, column + 1)
							;
					}
					else
					{	northwestCell = (column == 0)
							? west.edge(row - 1, gridSize - 1)
							: grid[row - 1][column - 1]
							;

						northCell = grid[row - 1][column];

						northeastCell = (column == gridSize - 1)
							? east.edge(row - 1, 0)
							: grid[row - 1][column + 1]
							;
					}

					westCell = (column == 0)
							? west.edge(row, gridSize - 1)
							: grid[row][column - 1]
							;

					eastCell = (column == gridSize - 1)
							? east.edge(row, 0)
							: grid[row][column + 1]
							;

					if( row == gridSize - 1 )
					{	southwestCell = (column == 0)
							? southwest.edge(0, gridSize - 1)
							: south.edge    (0, column - 1)
							;

						southCell = south.edge(0, column);

						southeastCell = (column == gridSize - 1)
							? southeast.edge(0, 0)
							: south.edge    (0, column + 1)
							;
					}
					else
					{	southwestCell = (column == 0)
							? west.edge(row + 1, gridSize - 1)
							: grid[row + 1][column - 1]
							;

						southCell = grid[row + 1][column];

						southeastCell = (column == gridSize - 1)
							? east.edge(row + 1, 0)
							: grid[row + 1][column + 1]
							;
					}

					if( grid[row][column].figureNextState
						( northCell,     southCell,
						  eastCell,      westCell,
						  northeastCell, northwestCell,
						  southeastCell, southwestCell
						)
					)
					{	nothingHappened = false;
					}
				}
			}
		}

		if( amActive && nothingHappened )
			oneLastRefreshRequired = true;

		amActive = !nothingHappened;
		return amActive;
	}

	public boolean transition()
	{
		boolean someSubcellChangedState = false;

		if( ++nestingLevel == 0 )
			readingPermitted.set(false);

		activeEdges.clear();

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
			{	if( grid[row][column].transition() )
				{	rememberThatCellAtEdgeChangedState(row, column);
					someSubcellChangedState = true;
				}
			}

		if( nestingLevel-- == 0 )
			readingPermitted.set(true);

		return someSubcellChangedState;
	}

	private static int nestingLevel = -1;

	private void rememberThatCellAtEdgeChangedState(int row, int column)
	{
		if( row == 0 )
		{	activeEdges.add( Direction.NORTH );

			if( column == 0 )
				activeEdges.add( Direction.NORTHWEST );
			else if( column == gridSize - 1 )
				activeEdges.add( Direction.NORTHEAST );
		}
		else if( row == gridSize - 1 )
		{	activeEdges.add( Direction.SOUTH );

			if( column == 0 )
				activeEdges.add( Direction.SOUTHWEST );
			else if( column == gridSize - 1 )
				activeEdges.add( Direction.SOUTHEAST );
		}

		if( column == 0 )
		{	activeEdges.add( Direction.WEST );
		}
		else if( column == gridSize - 1 )
		{	activeEdges.add( Direction.EAST );
		}
	}

	public void redraw(Graphics g, Rectangle here, boolean drawAll)
	{
		if( !amActive && !oneLastRefreshRequired && !drawAll )
			return;
		try
		{
			oneLastRefreshRequired = false;
			int compoundWidth = here.width;
			Rectangle subcell = new Rectangle( here.x, here.y,
											here.width  / gridSize,
											here.height / gridSize );

			if( !readingPermitted.isTrue() )
				return;

			readingPermitted.waitForTrue();

			for( int row = 0; row < gridSize; ++row )
			{	for( int column = 0; column < gridSize; ++column )
				{	grid[row][column].redraw( g, subcell, drawAll );
					subcell.translate( subcell.width, 0 );
				}
				subcell.translate(-compoundWidth, subcell.height);
			}

			g = g.create();
			g.setColor( Colors.LIGHT_ORANGE );
			g.drawRect( here.x, here.y, here.width, here.height );

			if( amActive )
			{	g.setColor( Color.BLUE );
				g.drawRect( here.x + 1,     here.y + 1,
							here.width - 2, here.height - 2 );
			}

			g.dispose();
		}
		catch( InterruptedException e )
		{
		}
	}

	public Cell edge(int row, int column)
	{	assert	  (row    == 0 || row    == gridSize - 1)
			   || (column == 0 || column == gridSize - 1)
			   :  "central cell requested from edge()";

		return grid[row][column];
	}

	public void userClicked(Point here, Rectangle surface)
	{
		int pixelsPerCell = surface.width / gridSize;
		int row           = here.y        / pixelsPerCell;
		int column        = here.x        / pixelsPerCell;
		int rowOffset     = here.y        % pixelsPerCell;
		int columnOffset  = here.x        % pixelsPerCell;

		Point position = new Point( columnOffset, rowOffset );
		Rectangle subcell = new Rectangle( 0, 0, pixelsPerCell, pixelsPerCell );

		grid[row][column].userClicked(position, subcell);
		amActive = true;
		rememberThatCellAtEdgeChangedState(row, column);
	}

	public boolean isAlive()
	{	return true;
	}

	public int widthInCells()
	{	return gridSize * grid[0][0].widthInCells();
	}

	public void clear()
	{	activeEdges.clear();

		for( int row = 0; row < gridSize; ++row )
			for( int column = 0; column < gridSize; ++column )
				grid[row][column].clear();

		amActive = false;
	}

	public boolean transfer(Storable memento, Point corner, boolean load)
	{	int   subcellWidth = grid[0][0].widthInCells();
		int   myWidth      = widthInCells();
		Point upperLeft    = new Point( corner );

		for( int row = 0; row < gridSize; ++row )
		{	for( int column = 0; column < gridSize; ++column )
			{	if( grid[row][column].transfer(memento, upperLeft, load) )
					amActive = true;

				Direction d = grid[row][column].isDisruptiveTo();

				if( !d.equals( Direction.NONE ) )
					activeEdges.add(d);

				upperLeft.translate( subcellWidth, 0 );
			}
			upperLeft.translate(-myWidth, subcellWidth);
		}
		return amActive;
	}

	public Storable createMemento()
	{	Memento m = new NeighborhoodState();
		transfer(m, new Point(0, 0), Cell.STORE);
		return m;
	}

	private static class NeighborhoodState implements Cell.Memento
	{	Collection liveCells = new LinkedList();

		public NeighborhoodState( InputStream in ) throws IOException
		{	load(in);
		}

		public NeighborhoodState()
		{
		}

		public void load( InputStream in ) throws IOException
		{	try
			{	ObjectInputStream source = new ObjectInputStream( in );
				liveCells = (Collection)( source.readObject() );
			}
			catch( ClassNotFoundException e )
			{	throw new IOException(
							"Internal Error: Class not found on load");
			}
		}

		public void flush( OutputStream out ) throws IOException
		{	ObjectOutputStream sink = new ObjectOutputStream(out);
			sink.writeObject( liveCells );
		}

		public void markAsAlive(Point location)
		{	liveCells.add( new Point( location ) );
		}

		public boolean isAlive(Point location)
		{	return liveCells.contains(location);
		}

		public String toString()
		{	StringBuffer b = new StringBuffer();

			b.append("NeighborhoodState:\n");
			for( Iterator i = liveCells.iterator(); i.hasNext(); )
				b.append( ((Point) i.next()).toString() + "\n" );
			return b.toString();
		}
	}

	/**
	 * Accepts a CellVisitor for double-dispatch traversal.
	 */
	public void accept( CellVisitor visitor )
	{	visitor.visit( this );
	}

	/**
	 * Returns the internal grid of child cells for visitor traversal.
	 */
	public Cell[][] getGrid()
	{	return grid;
	}

	/**
	 * Returns the dimension (rows/columns) of this neighborhood grid.
	 */
	public int getGridSize()
	{	return gridSize;
	}

	/**
	 * Marks this neighborhood as active so the next generation is computed.
	 */
	public void markActive()
	{	amActive = true;
	}
}
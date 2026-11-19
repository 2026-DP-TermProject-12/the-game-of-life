package com.holub.life;

/**
 * Traverses the Cell hierarchy to collect live cell count
 * and a hash representing the current board layout.
 */
public class ScoreVisitor implements CellVisitor
{
	private int  liveCount = 0;
	private long boardHash = 17L;
	private int  cellIndex = 0;

	public void visit( Resident cell )
	{	cellIndex++;
		if( cell.isAlive() )
		{	liveCount++;
			boardHash = boardHash * 31L + cellIndex;
		}
	}

	public void visit( Neighborhood grid )
	{	Cell[][] children = grid.getGrid();
		int size = grid.getGridSize();

		for( int row = 0; row < size; row++ )
		{	for( int col = 0; col < size; col++ )
			{	children[row][col].accept( this );
			}
		}
	}

	public int getLiveCount()
	{	return liveCount;
	}

	public long getBoardHash()
	{	return boardHash;
	}
}
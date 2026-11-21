package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;

import com.holub.life.LifePattern.Position;

/**
 * Adds a pattern's live cells to a square game board at a cell-based origin.
 * The board's memento is used to preserve existing live cells. New live cells
 * go through the board's usual click path so it can track changed neighbors.
 */
final class PatternPlacer
{
	/**
	 * Places the pattern with its upper-left corner at the requested cell. The
	 * entire pattern rectangle must fit, including any empty rows or columns.
	 * Cells outside the pattern and cells already alive keep their current state.
	 *
	 * @param board outermost cell of a square board that supplies a {@link Cell.Memento}
	 * @param pattern pattern to place in its current orientation
	 * @param column zero-based origin column on the board
	 * @param row zero-based origin row on the board
	 * @return {@code true} if the pattern fits, or {@code false} if no cells were placed
	 *         because the pattern was out of bounds
	 */
	boolean place( Cell board, LifePattern pattern, int column, int row )
	{	int boardSize = board.widthInCells();
		// Reject the whole placement before touching any board cell.
		if( column < 0 || row < 0
				|| column > boardSize - pattern.width() || row > boardSize - pattern.height() )
			return false;

		// Read the original state because clicking a live cell would turn it off.
		Cell.Memento currentState = (Cell.Memento) board.createMemento();
		// Use a logical surface with one pixel per cell to reuse the click path.
		Rectangle surface = new Rectangle( 0, 0, boardSize, boardSize );
		// Translate each relative position into an absolute board cell.
		for( Position position : pattern.liveCells() )
		{	Point target = new Point( column + position.column(), row + position.row() );
			if( !currentState.isAlive( target ) )
				board.userClicked( target, surface );
		}
		return true;
	}
}

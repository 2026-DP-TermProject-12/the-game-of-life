package com.holub.life;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a rectangular Game of Life pattern as dimensions and a list of
 * live cells. Coordinates start at the upper-left corner, with columns growing
 * to the right and rows growing downward. Positions absent from the list are
 * dead. Rotation and reflection change the current instance, so callers can
 * transform a {@link #copy()} independently of the original.
 */
final class LifePattern
{
	/**
	 * An immutable position relative to the upper-left corner of a grid.
	 *
	 * @param column zero-based horizontal offset
	 * @param row zero-based vertical offset
	 */
	record Position( int column, int row )
	{
	}

	private     final List<Position> 	liveCells;
	private 		  int		 		width;
	private 		  int 				height;

	/**
	 * Creates a pattern from dimensions and distinct live positions within them.
	 * The constructor copies the list so later changes to it cannot affect this pattern.
	 *
	 * @param width positive number of columns in the pattern
	 * @param height positive number of rows in the pattern
	 * @param liveCells live positions within the pattern bounds
	 */
	LifePattern( int width, int height, List<Position> liveCells )
	{	this.width = width;
		this.height = height;
		// Own the list so in-place transforms cannot change the caller's data.
		this.liveCells = new ArrayList<>( liveCells );
	}

	/**
	 * Copies the current dimensions and live positions into an independent pattern.
	 *
	 * @return a pattern whose transformations do not affect this instance
	 */
	LifePattern copy()
	{	return new LifePattern( width, height, liveCells ); // The constructor copies the list.
	}

	/**
	 * Rotates this pattern 90 degrees clockwise in place. Live cells are mapped
	 * into a new local coordinate system, and the width and height are swapped.
	 */
	void rotateClockwise()
	{	liveCells.replaceAll( position ->
				new Position( height - 1 - position.row(), position.column() ) ); // Use the old height.
		// Swap dimensions only after remapping all positions.
		int previousWidth = width;
		width = height;
		height = previousWidth;
	}

	/**
	 * Reflects this pattern left to right in place. Rows and dimensions stay the same.
	 */
	void flipHorizontally()
	{	liveCells.replaceAll( position ->
				new Position( width - 1 - position.column(), position.row() ) ); // Mirror only the column.
	}

	/** @return the number of columns in the current orientation */
	int width()
	{	return width; // The extent is already updated by rotation.
	}

	/** @return the number of rows in the current orientation */
	int height()
	{	return height; // The extent includes empty rows inside the pattern.
	}

	/**
	 * Exposes the live positions in their current orientation for reading.
	 *
	 * @return an unmodifiable view that reflects subsequent transformations of this instance
	 */
	List<Position> liveCells()
	{	return Collections.unmodifiableList( liveCells ); // Keep structural changes inside this class.
	}
}

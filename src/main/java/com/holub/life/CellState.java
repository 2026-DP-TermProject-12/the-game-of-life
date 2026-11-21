package com.holub.life;

/**
 * Represents the life state of a Resident cell.
 */
public interface CellState
{
	/**
	 * Computes the next state based on the number of living neighbors.
	 */
	CellState computeNextState( int liveNeighbors );

	/**
	 * Returns whether the cell is currently alive.
	 */
	boolean isAlive();

	/**
	 * Returns the opposite state when toggled by user interaction.
	 */
	CellState toggle();
}
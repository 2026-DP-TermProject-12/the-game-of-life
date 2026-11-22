package com.holub.life;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Draws the appearance of one cell. A cell only reports whether it is
 * alive, so the look of the board can be replaced without touching the
 * state-handling code.
 */
interface CellRenderer
{
	/**
	 * Paints a single cell in its current state.
	 *
	 * @param g graphics to paint on
	 * @param here bounds of the cell in the coordinate space of {@code g}
	 * @param alive true if the cell being drawn is alive
	 */
	void draw( Graphics g, Rectangle here, boolean alive );
}

package com.holub.life;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Keeps the presentation side of a single cell: the appearance it was
 * painted with last and whether it still has to be painted at all. The
 * cell supplies only its current state, so nothing here has to know how
 * that state was computed.
 */
final class ResidentView
{
	private final CellRenderer renderer;

	/** True once this view has painted at least one frame, so that a
	 *  cell is never skipped before it has been drawn for the first time.
	 */
	private boolean painted = false;
	private boolean lastPaintedAlive = false;

	ResidentView()
	{	this( DefaultCellRenderer.instance() );
	}

	ResidentView( CellRenderer renderer )
	{	this.renderer = renderer;
	}

	/**
	 * Paints the cell unless its appearance is unchanged since the last paint.
	 *
	 * @param g graphics to paint on
	 * @param here bounds of the cell
	 * @param alive state the cell is in now
	 * @param drawAll true to paint even if the appearance did not change
	 */
	void redraw( Graphics g, Rectangle here, boolean alive, boolean drawAll )
	{	if( painted && !drawAll && alive == lastPaintedAlive )
			return;

		renderer.draw( g, here, alive );

		painted = true;
		lastPaintedAlive = alive;
	}
}

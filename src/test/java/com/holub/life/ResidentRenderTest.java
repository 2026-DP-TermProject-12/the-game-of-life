package com.holub.life;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks that the rendering extracted from {@link Resident} still paints
 * the same appearance and now skips a paint when nothing changed.
 */
class ResidentRenderTest
{
	/** Size of the one cell used by the tests, in pixels. */
	private static final int CELL = 10;

	/** A cell-sized image filled with a color that is never a cell color,
	 *  so that a skipped paint can be detected.
	 */
	private BufferedImage blank()
	{	BufferedImage image = new BufferedImage( CELL, CELL, BufferedImage.TYPE_INT_RGB );
		Graphics2D g = image.createGraphics();
		g.setColor( Color.BLACK );
		g.fillRect( 0, 0, CELL, CELL );
		g.dispose();
		return image;
	}

	private Color pixel( BufferedImage image )
	{	return new Color( image.getRGB( CELL/2, CELL/2 ) );
	}

	private Rectangle bounds()
	{	return new Rectangle( 0, 0, CELL, CELL );
	}

	@Test
	void deadCellPaintsItsDeadColor()
	{	Resident cell = new Resident();
		BufferedImage image = blank();
		cell.redraw( image.createGraphics(), bounds(), true );
		assertEquals( com.holub.ui.Colors.LIGHT_YELLOW, pixel( image ) );
	}

	@Test
	void liveCellPaintsItsLiveColor()
	{	Resident cell = new Resident();
		cell.userClicked( new Point( 0, 0 ), bounds() );	// Make it alive.
		BufferedImage image = blank();
		cell.redraw( image.createGraphics(), bounds(), true );
		assertEquals( Color.RED, pixel( image ) );
	}

	@Test
	void borderIsPaintedOnTheTopAndLeftEdges()
	{	Resident cell = new Resident();
		BufferedImage image = blank();
		cell.redraw( image.createGraphics(), bounds(), true );
		assertEquals( com.holub.ui.Colors.DARK_YELLOW,
					  new Color( image.getRGB( 0, CELL/2 ) ) );	// Left edge.
		assertEquals( com.holub.ui.Colors.DARK_YELLOW,
					  new Color( image.getRGB( CELL/2, 0 ) ) );	// Top edge.
	}

	@Test
	void unchangedCellIsNotPaintedAgain()
	{	Resident cell = new Resident();
		BufferedImage image = blank();
		cell.redraw( image.createGraphics(), bounds(), true );	// First paint.
		assertEquals( com.holub.ui.Colors.LIGHT_YELLOW, pixel( image ) );

		// Overwrite the cell and ask for a conditional repaint.
		Graphics2D g = image.createGraphics();
		g.setColor( Color.BLACK );
		g.fillRect( 0, 0, CELL, CELL );
		g.dispose();
		cell.redraw( image.createGraphics(), bounds(), false );

		assertEquals( Color.BLACK, pixel( image ) );	// Nothing was drawn.
	}

	@Test
	void changedCellIsPaintedEvenOnConditionalRedraw()
	{	Resident cell = new Resident();
		BufferedImage image = blank();
		cell.redraw( image.createGraphics(), bounds(), true );	// Dead, first paint.

		cell.userClicked( new Point( 0, 0 ), bounds() );		// Now alive.
		cell.redraw( image.createGraphics(), bounds(), false );

		assertEquals( Color.RED, pixel( image ) );
	}

	@Test
	void fullRedrawIgnoresTheSkip()
	{	Resident cell = new Resident();
		BufferedImage image = blank();
		cell.redraw( image.createGraphics(), bounds(), true );	// First paint.

		Graphics2D g = image.createGraphics();
		g.setColor( Color.BLACK );
		g.fillRect( 0, 0, CELL, CELL );
		g.dispose();
		cell.redraw( image.createGraphics(), bounds(), true );	// drawAll.

		assertEquals( com.holub.ui.Colors.LIGHT_YELLOW, pixel( image ) );
	}

	@Test
	void stateHandlingIsUnaffectedByTheSplit()
	{	Resident cell = new Resident();
		assertFalse( cell.isAlive() );

		cell.userClicked( new Point( 0, 0 ), bounds() );
		assertTrue( cell.isAlive() );

		cell.clear();
		assertFalse( cell.isAlive() );

		assertEquals( Direction.NONE, cell.isDisruptiveTo() );
	}
}
D

package com.holub.life;

import com.holub.ui.Colors;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RendererSelectionTest
{
	private static final Rectangle CELL = new Rectangle( 0, 0, 20, 20 );

	@Test
	void switchingStrategyRepaintsUnchangedCellsAndErasesOldShapes()
	{	RendererSelection selection = new RendererSelection( DefaultCellRenderer.instance() );
		Resident cell = new Resident( selection );
		cell.toggleState();
		BufferedImage image = new BufferedImage( 20, 20, BufferedImage.TYPE_INT_RGB );
		Graphics2D g = image.createGraphics();
		try
		{	cell.redraw( g, CELL, false );
			assertEquals( Color.RED.getRGB(), image.getRGB( 2, 2 ) );

			selection.select( new GreenCircleRenderer() );
			cell.redraw( g, CELL, false );
			assertEquals( Color.GREEN.getRGB(), image.getRGB( 10, 10 ) );
			assertEquals( Colors.LIGHT_YELLOW.getRGB(), image.getRGB( 2, 2 ) );

			selection.select( new BlueTriangleRenderer() );
			cell.redraw( g, CELL, false );
			assertEquals( Color.BLUE.getRGB(), image.getRGB( 10, 10 ) );
			assertEquals( Colors.LIGHT_YELLOW.getRGB(), image.getRGB( 3, 7 ) );

			cell.clear();
			cell.redraw( g, CELL, false );
			assertEquals( Colors.LIGHT_YELLOW.getRGB(), image.getRGB( 10, 10 ) );

			cell.toggleState();
			selection.select( DefaultCellRenderer.instance() );
			cell.redraw( g, CELL, false );
			assertEquals( Color.RED.getRGB(), image.getRGB( 2, 2 ) );
			assertEquals( Colors.DARK_YELLOW.getRGB(), image.getRGB( 0, 10 ) );
		}
		finally
		{	g.dispose();
		}
	}

	@Test
	void nestedCopiesShareSelectionButKeepIndependentPaintHistory()
	{	RendererSelection selection = new RendererSelection( DefaultCellRenderer.instance() );
		Neighborhood board = new Neighborhood( 2,
			new Neighborhood( 2, new Resident( selection ) ) );
		Rectangle bounds = new Rectangle( 0, 0, 80, 80 );
		board.userClicked( new Point( 21, 21 ), bounds );
		ScoreVisitor before = new ScoreVisitor();
		board.accept( before );
		BufferedImage image = new BufferedImage( 80, 80, BufferedImage.TYPE_INT_RGB );
		Graphics2D g = image.createGraphics();
		try
		{	board.redraw( g, bounds, true );
			AtomicInteger draws = new AtomicInteger();
			selection.select( (graphics, here, alive) -> {
				draws.incrementAndGet();
				new GreenCircleRenderer().draw( graphics, here, alive );
			} );
			board.redraw( g, bounds, true );
			assertEquals( 16, draws.get() );
			assertEquals( Color.GREEN.getRGB(), image.getRGB( 30, 30 ) );
			assertEquals( Colors.LIGHT_YELLOW.getRGB(), image.getRGB( 10, 10 ) );
			board.redraw( g, bounds, false );
			assertEquals( 16, draws.get() );

			ScoreVisitor after = new ScoreVisitor();
			board.accept( after );
			assertEquals( before.getLiveCount(), after.getLiveCount() );
			assertEquals( before.getBoardState(), after.getBoardState() );
		}
		finally
		{	g.dispose();
		}
	}

}

package com.holub.life;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import com.holub.ui.Colors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NeighborhoodRenderTest
{
	private static final Rectangle BOUNDS = new Rectangle( 0, 0, 80, 80 );

	@Test
	void emptyBlockErasesItsPreviousActiveBorder()
	{	Neighborhood board = new Neighborhood( 8, new Resident() );
		assertStableBorderIsErased( board, false );
	}

	@Test
	void stablePatternErasesBordersAcrossNestedBlocks()
	{	Neighborhood board = new Neighborhood( 2, new Neighborhood( 4, new Resident() ) );
		assertStableBorderIsErased( board, true );
	}

	private void assertStableBorderIsErased( Neighborhood board, boolean keepBlock )
	{	board.userClicked( new Point( 21, 21 ), BOUNDS );
		if( keepBlock )
		{	board.userClicked( new Point( 31, 21 ), BOUNDS );
			board.userClicked( new Point( 21, 31 ), BOUNDS );
			board.userClicked( new Point( 31, 31 ), BOUNDS );
		}
		else
			board.userClicked( new Point( 21, 21 ), BOUNDS );

		BufferedImage image = new BufferedImage( 80, 80, BufferedImage.TYPE_INT_RGB );
		Graphics2D g = image.createGraphics();
		try
		{	board.redraw( g, BOUNDS, true );
			assertEquals( Color.BLUE.getRGB(), image.getRGB( 1, 5 ) );

			assertFalse( new LifeBoard( board ).step() );
			board.redraw( g, BOUNDS, false );
			assertEquals( Colors.LIGHT_YELLOW.getRGB(), image.getRGB( 1, 5 ) );

			BufferedImage expected = new BufferedImage( 80, 80, BufferedImage.TYPE_INT_RGB );
			Graphics2D full = expected.createGraphics();
			try
			{	board.redraw( full, BOUNDS, true );
			}
			finally
			{	full.dispose();
			}

			assertArrayEquals( expected.getRGB( 0, 0, 80, 80, null, 0, 80 ),
				image.getRGB( 0, 0, 80, 80, null, 0, 80 ) );
			assertEquals( keepBlock, board.hasLivingCells() );
		}
		finally
		{	g.dispose();
		}
	}
}

package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import com.holub.ui.Colors;

/**
 * The standard appearance of a cell: a red square while alive, a pale
 * yellow square while dead, and a thin border along the top and left
 * edges. It holds no state, so every cell can share one instance.
 */
final class DefaultCellRenderer implements CellRenderer
{
	private static final CellRenderer INSTANCE = new DefaultCellRenderer();

	private static final Color BORDER_COLOR = Colors.DARK_YELLOW;
	private static final Color LIVE_COLOR   = Color.RED;
	private static final Color DEAD_COLOR   = Colors.LIGHT_YELLOW;

	private DefaultCellRenderer()
	{	// Nothing to initialize: every cell shares the instance().
	}

	/** @return the shared renderer used by every cell */
	static CellRenderer instance()
	{	return INSTANCE;
	}

	public void draw( Graphics g, Rectangle here, boolean alive )
	{	g = g.create();
		g.setColor(alive ? LIVE_COLOR : DEAD_COLOR );
		g.fillRect(here.x+1, here.y+1, here.width-1, here.height-1);

		// Doesn't draw a line on the far right and bottom of the
		// grid, but that's life, so to speak. It's not worth the
		// code for the special case.
		g.setColor( BORDER_COLOR );
		g.drawLine( here.x, here.y, here.x, here.y + here.height );
		g.drawLine( here.x, here.y, here.x + here.width, here.y  );
		g.dispose();
	}
}

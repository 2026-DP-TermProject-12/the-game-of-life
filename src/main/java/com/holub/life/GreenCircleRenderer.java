package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** Draws living cells as green circles on the standard empty grid. */
final class GreenCircleRenderer implements CellRenderer
{
	public void draw( Graphics g, Rectangle here, boolean alive )
	{	DefaultCellRenderer.instance().draw( g, here, false );
		if( !alive )
			return;

		Graphics copy = g.create();
		try
		{	copy.setColor( Color.GREEN );
			copy.fillOval( here.x + 1, here.y + 1, here.width - 1, here.height - 1 );
		}
		finally
		{	copy.dispose();
		}
	}
}

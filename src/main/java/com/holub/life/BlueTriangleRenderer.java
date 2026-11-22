package com.holub.life;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/** Draws living cells as blue triangles on the standard empty grid. */
final class BlueTriangleRenderer implements CellRenderer
{
	public void draw( Graphics g, Rectangle here, boolean alive )
	{	DefaultCellRenderer.instance().draw( g, here, false );
		if( !alive )
			return;

		Graphics copy = g.create();
		try
		{	copy.setColor( Color.BLUE );
			int left = here.x + 1;
			int right = here.x + here.width - 1;
			int top = here.y + 1;
			int bottom = here.y + here.height - 1;
			copy.fillPolygon( new int[]{ (left + right) / 2, left, right },
				new int[]{ top, bottom, bottom }, 3 );
		}
		finally
		{	copy.dispose();
		}
	}
}

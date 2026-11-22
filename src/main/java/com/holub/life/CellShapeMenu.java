package com.holub.life;

import com.holub.ui.MenuSite;

/** Installs the rendering strategy menu independently of cell state handling. */
final class CellShapeMenu
{
	private static final String MENU = "Cell Shape";
	private final RendererSelection renderers;
	private final Runnable repaint;

	CellShapeMenu( RendererSelection renderers, Runnable repaint )
	{	this.renderers = renderers;
		this.repaint = repaint;
	}

	void install()
	{	add( "Red Square", DefaultCellRenderer.instance() );
		add( "Green Circle", new GreenCircleRenderer() );
		add( "Blue Triangle", new BlueTriangleRenderer() );
		updateLabel( "Red Square" );
	}

	private void add( String label, CellRenderer renderer )
	{	MenuSite.addLine( this, MENU, label, event -> {
			renderers.select( renderer );
			updateLabel( label );
			repaint.run();
		} );
	}

	private void updateLabel( String label )
	{	MenuSite.getMyMenuItem( this, MENU, null ).setText( MENU + " (" + label + ")" );
	}
}

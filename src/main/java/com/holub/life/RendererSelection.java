package com.holub.life;

import java.util.Objects;

/** Holds the rendering strategy shared by all cells on one board. */
final class RendererSelection
{
	private CellRenderer renderer;

	RendererSelection( CellRenderer renderer )
	{	this.renderer = Objects.requireNonNull( renderer );
	}

	CellRenderer renderer()
	{	return renderer;
	}

	void select( CellRenderer renderer )
	{	this.renderer = Objects.requireNonNull( renderer );
	}
}

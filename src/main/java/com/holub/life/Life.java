package com.holub.life;

import com.holub.ui.MenuSite;

import javax.swing.*;
import java.awt.*;

/*******************************************************************
 * An implemenation of Conway's Game of Life.
 *
 * @include /etc/license.txt
 */

public final class Life extends JFrame
{	
	private static JComponent universe;

	public static void main( String[] arguments )
	{	SwingUtilities.invokeLater( Life::new );
	}

	private Life()
	{	super( "The Game of Life. "
					+"(c)2003 Allen I. Holub <http://www.holub.com>");

		// Must establish the MenuSite very early in case
		// a subcomponent puts menus on it.
		MenuSite.establish( this );		//{=life.java.establish}

        setDefaultCloseOperation( EXIT_ON_CLOSE );
        getContentPane().setLayout( new BorderLayout() );

        Universe board = Universe.instance();
        getContentPane().add( board.statusBar(), BorderLayout.NORTH );
        getContentPane().add( board, BorderLayout.CENTER );

		// The clock no longer creates its own menu. Install it after
		// the Universe so the "Go" menu still follows the "Grid" menu.
		new ClockMenu( Clock.instance() ).install();

		pack();
		setVisible( true );
	}
}

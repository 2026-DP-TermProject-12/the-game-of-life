package com.holub.life;

import com.holub.ui.MenuSite;
import java.awt.GraphicsEnvironment;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class HistoryMenuTest
{
	@Test void playbackLocksBothMenusAndHaltRestoresAvailableActions() throws Exception
	{	assumeFalse( GraphicsEnvironment.isHeadless(), "Swing menus require a display" );
		SwingUtilities.invokeAndWait( () -> {
			JFrame frame = new JFrame();
			try
			{	MenuSite.establish( frame );
				Universe board = Universe.instance();
				ClockMenu clock = new ClockMenu( Clock.instance() );
				clock.install();
				JMenuItem undo = MenuSite.getMyMenuItem( board, "Grid", "Undo" );
				JMenuItem redo = MenuSite.getMyMenuItem( board, "Grid", "Redo" );
				JMenuItem halt = MenuSite.getMyMenuItem( clock, "Go", "Halt" );
				JMenuItem step = MenuSite.getMyMenuItem( clock, "Go", "Tick (Single Step)" );

				assertFalse( undo.isEnabled() );
				assertFalse( redo.isEnabled() );
				step.doClick( 0 );
				assertTrue( undo.isEnabled() );
				assertFalse( redo.isEnabled() );
				step.doClick( 0 );
				undo.doClick( 0 );
				assertTrue( undo.isEnabled() );
				assertTrue( redo.isEnabled() );

				// Stay on the event thread so no timer tick intervenes before Halt.
				for( String speed : new String[]{ "Agonizing", "Slow", "Medium", "Fast" } )
				{	MenuSite.getMyMenuItem( clock, "Go", speed ).doClick( 0 );
					assertFalse( undo.isEnabled(), speed );
					assertFalse( redo.isEnabled(), speed );
					undo.doClick( 0 );
					halt.doClick( 0 );
					assertTrue( undo.isEnabled(), speed );
					assertTrue( redo.isEnabled(), speed );
				}

				redo.doClick( 0 );
				assertTrue( undo.isEnabled() );
				assertFalse( redo.isEnabled() );

				String boundaryMenu = "Grid:Boundary";
				JMenuItem label = MenuSite.getMyMenuItem( board, boundaryMenu, null );
				MenuSite.getMyMenuItem( board, boundaryMenu, "Wrap Around" ).doClick( 0 );
				assertEquals( "Boundary (Wrap Around)", label.getText() );
				undo.doClick( 0 );
				assertEquals( "Boundary (Fixed)", label.getText() );
				assertTrue( redo.isEnabled() );
				redo.doClick( 0 );
				assertEquals( "Boundary (Wrap Around)", label.getText() );

				JMenu shapes = null;
				for( int i = 0; i < frame.getJMenuBar().getMenuCount(); ++i )
				{	JMenu candidate = frame.getJMenuBar().getMenu( i );
					if( candidate.getText().startsWith( "Cell Shape (" ) )
						shapes = candidate;
				}
				assertNotNull( shapes );
				assertEquals( 3, shapes.getItemCount() );
				Clock.instance().startTicking( 10000 );
				String[] shapeLabels = { "Red Square", "Green Circle", "Blue Triangle" };
				for( int i : new int[]{ 1, 2, 0 } )
				{	assertTrue( shapes.getItem( i ).isEnabled() );
					shapes.getItem( i ).doClick( 0 );
					assertEquals( "Cell Shape (" + shapeLabels[i] + ")", shapes.getText() );
					assertTrue( Clock.instance().isRunning() );
				}
			}
			finally
			{	Clock.instance().stop();
				frame.dispose();
			}
		} );
	}
}

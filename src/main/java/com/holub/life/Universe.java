package com.holub.life;

import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

import com.holub.io.Files;
import com.holub.ui.MenuSite;
import com.holub.ui.Colors;

import com.holub.life.Cell;
import com.holub.life.Storable;
import com.holub.life.Clock;
import com.holub.life.Neighborhood;
import com.holub.life.Resident;

/**
 * The Universe is a mediator that sits between the Swing
 * event model and the Life classes.
 *
 * @include /etc/license.txt
 */
public class Universe extends JPanel
{
	private final Cell         outermostCell;
	private final ScoreTracker scoreTracker = new ScoreTracker();

	private static final Universe theInstance = new Universe();

	private static final int DEFAULT_GRID_SIZE = 8;
	private static final int DEFAULT_CELL_SIZE = 8;

	private Universe()
	{
		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident()
							)
						);

		final Dimension PREFERRED_SIZE =
						new Dimension
						(	outermostCell.widthInCells() * DEFAULT_CELL_SIZE,
							outermostCell.widthInCells() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized( ComponentEvent e )
				{	Rectangle bounds = getBounds();
					bounds.height /= outermostCell.widthInCells();
					bounds.height *= outermostCell.widthInCells();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white    );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true           );

		addMouseListener
		(	new MouseAdapter()
			{	public void mousePressed( MouseEvent e )
				{	Rectangle bounds = getBounds();
					bounds.x = 0;
					bounds.y = 0;
					outermostCell.userClicked( e.getPoint(), bounds );
					scoreTracker.reset();
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Clear",
			new ActionListener()
			{	public void actionPerformed( ActionEvent e )
				{	outermostCell.clear();
					scoreTracker.reset();
					repaint();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Load",
			new ActionListener()
			{	public void actionPerformed( ActionEvent e )
				{	doLoad();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Store",
			new ActionListener()
			{	public void actionPerformed( ActionEvent e )
				{	doStore();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Exit",
			new ActionListener()
			{	public void actionPerformed( ActionEvent e )
				{	System.exit( 0 );
				}
			}
		);

		Clock.instance().addClockListener
		(	new Clock.Listener()
			{	public void tick()
				{	if( outermostCell.figureNextState
						( Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
						  Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY
						)
					)
					{	if( outermostCell.transition() )
							refreshNow();
					}

					scoreTracker.evaluate( outermostCell );
					refreshNow();
				}
			}
		);
	}

	public static Universe instance()
	{	return theInstance;
	}

	private void doLoad()
	{	try
		{
			FileInputStream in = new FileInputStream(
				Files.userSelected( ".", ".life", "Life File", "Load" ) );

			Clock.instance().stop();
			outermostCell.clear();

			Storable memento = outermostCell.createMemento();
			memento.load( in );
			outermostCell.transfer( memento, new Point( 0, 0 ), Cell.LOAD );

			in.close();
			scoreTracker.reset();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Read Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE );
		}
		repaint();
	}

	private void doStore()
	{	try
		{
			FileOutputStream out = new FileOutputStream(
				Files.userSelected( ".", ".life", "Life File", "Store" ) );

			Clock.instance().stop();

			Storable memento = outermostCell.createMemento();
			outermostCell.transfer( memento, new Point( 0, 0 ), Cell.STORE );
			memento.flush( out );

			out.close();
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Write Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE );
		}
	}

	public void paint( Graphics panel )
	{
		Rectangle panelBounds = getBounds();
		Rectangle clipBounds  = panel.getClipBounds();

		panelBounds.x = 0;
		panelBounds.y = 0;
		outermostCell.redraw( panel, panelBounds, true );
		drawScoreHud( panel );
	}

	private void refreshNow()
	{	SwingUtilities.invokeLater
		(	new Runnable()
			{	public void run()
				{	Graphics g = getGraphics();
					if( g == null )
						return;
					try
					{	Rectangle panelBounds = getBounds();
						panelBounds.x = 0;
						panelBounds.y = 0;
						outermostCell.redraw( g, panelBounds, false );
						drawScoreHud( g );
					}
					finally
					{	g.dispose();
					}
				}
			}
		);
	}

	/**
	 * Draws a score HUD overlay at the top-left of the board.
	 */
	private void drawScoreHud( Graphics g )
	{	Graphics2D g2 = (Graphics2D) g.create();
		try
		{	String text = String.format(
				"Score: %d  |  Gen: %d  |  Status: %s",
				scoreTracker.getScore(),
				scoreTracker.getGeneration(),
				scoreTracker.getStatus() );

			g2.setFont( new Font( Font.SANS_SERIF, Font.BOLD, 12 ) );
			FontMetrics fm = g2.getFontMetrics();
			int boxWidth  = Math.max( fm.stringWidth( text ) + 20, 290 );
			int boxHeight = fm.getHeight() + 8;

			g2.setColor( new Color( 30, 30, 30 ) );
			g2.fillRoundRect( 8, 8, boxWidth, boxHeight, 8, 8 );

			g2.setColor( Color.WHITE );
			g2.drawString( text, 18, 8 + fm.getAscent() + 4 );
		}
		finally
		{	g2.dispose();
		}
	}
}
package com.holub.life;

import com.holub.io.Files;
import com.holub.ui.MenuSite;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * The Universe is a mediator that sits between the Swing
 * event model and the Life classes. It is also a singleton,
 * accessed via Universe.instance(). It handles all
 * Swing events and translates them into requests to the
 * outermost Neighborhood. It also creates the Composite
 * Neighborhood.
 *
 * @include /etc/license.txt
 */

public class Universe extends JPanel
{
	private final         Neighborhood      outermostCell;
	private final         LifeBoard         board;

	private static  final String            BOUNDARY_MENU = "Grid:Boundary";
	private static	final Universe 	        theInstance = new Universe();

	private 		final PatternCatalog 	patternCatalog = new PatternCatalog();
	private			final PatternPlacer 	patternPlacer = new PatternPlacer();
	private 		final Object 			boardLock = new Object();

    private 			  LifePattern 		pendingPattern;
	private 			  JMenuItem 		editPresetItem;

	/** The default height and width of a Neighborhood in cells.
	 *  If it's too big, you'll run too slowly because
	 *  you have to update the entire block as a unit, so there's more
	 *  to do. If it's too small, you have too many blocks to check.
	 *  I've found that 8 is a good compromise.
	 */
	private static final int  DEFAULT_GRID_SIZE = 8;

	/** The size of the smallest "atomic" cell---a Resident object.
	 *  This size is extrinsic to a Resident (It's passed into the
	 *  Resident's "draw yourself" method.
	 */
	private static final int  DEFAULT_CELL_SIZE = 8;

	// The constructor is private so that the universe can be created
	// only by an outer-class method [Neighborhood.createUniverse()].

	private Universe()
	{	// Create the nested Cells that comprise the "universe." A bug
		// in the current implementation causes the program to fail
		// miserably if the overall size of the grid is too big to fit
		// on the screen.

		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident()
							)
						);

		board = new LifeBoard( outermostCell );

		final Dimension PREFERRED_SIZE =
						new Dimension
						(  outermostCell.widthInCells() * DEFAULT_CELL_SIZE,
						   outermostCell.widthInCells() * DEFAULT_CELL_SIZE
						);

		addComponentListener
		(	new ComponentAdapter()
			{	public void componentResized(ComponentEvent e)
				{
					// Make sure that the cells fit evenly into the
					// total grid size so that each cell will be the
					// same size. For example, in a 64x64 grid, the
					// total size must be an even multiple of 63.

					Rectangle bounds = getBounds();
					bounds.height /= outermostCell.widthInCells();
					bounds.height *= outermostCell.widthInCells();
					bounds.width  =  bounds.height;
					setBounds( bounds );
				}
			}
		);

		setBackground	( Color.white	 );
		setPreferredSize( PREFERRED_SIZE );
		setMaximumSize	( PREFERRED_SIZE );
		setMinimumSize	( PREFERRED_SIZE );
		setOpaque		( true			 );

		addMouseListener					//{=Universe.mouse}
		(	new MouseAdapter()
			{
				/**
				 * Places the selected preset or forwards the click to the board for a cell toggle.
				 *
				 * @param e mouse press with coordinates relative to the game panel
				 */
				@Override
				public void mousePressed( MouseEvent e )
				{	if( pendingPattern != null )
					{	placePendingPattern( e.getPoint() ); // Consume the click as a placement attempt.
						return;
					}

					// The board expects bounds relative to the panel, rather than its parent.
					Rectangle bounds = getBounds();
					bounds.x = 0;
					bounds.y = 0;
                    synchronized( boardLock )
                    {   outermostCell.userClicked( e.getPoint(), bounds );
                    }
					repaint();
				}
			}
		);

        MenuSite.addLine( this, "Grid", "Clear", event -> {
            synchronized( boardLock )
            {
                outermostCell.clear();
            }
            repaint();
        } );

        MenuSite.addLine( this, "Grid", "Load", event -> doLoad() );
        MenuSite.addLine( this, "Grid", "Store", event -> doStore() );
        createPresetMenu(); // Register preset menu
        MenuSite.addLine( this, "Grid", "Exit",
                event -> System.exit( 0 ) );

		MenuSite.addLine( this, BOUNDARY_MENU, "Fixed",
				e -> selectBoundary( BoundaryPolicy.FIXED, "Fixed" ) );
        MenuSite.addLine( this, BOUNDARY_MENU, "Wrap Around",
				e -> selectBoundary( BoundaryPolicy.WRAP_AROUND, "Wrap Around" ) );
        updateBoundaryMenuLabel( "Fixed" );

        Clock.instance().addClockListener( this::advanceGeneration );
	}

	private void selectBoundary( BoundaryPolicy policy, String label )
	{	Clock.instance().stop();
        synchronized( boardLock )
        {   board.setBoundaryPolicy( policy );
        }
        updateBoundaryMenuLabel( label );
	}

	private void updateBoundaryMenuLabel( String label )
	{	MenuSite.getMyMenuItem( this, BOUNDARY_MENU, null ).setText( "Boundary (" + label + ")" );
	}

	/** Singleton Accessor. The Universe object itself is manufactured
	 *  in Neighborhood.createUniverse()
	 */

	public static Universe instance()
	{	return theInstance;
	}

    /**
	 * Registers the Grid preset submenu in catalog order, followed by actions
	 * that act on the current selection. Editing starts disabled until a pattern
	 * is selected.
	 */
	private void createPresetMenu()
	{	String menu = "Grid:Preset";

		// Bind each name to the action that requests its own working copy.
		for( String name : patternCatalog.names() )
			MenuSite.addLine( this, menu, name, event -> selectPattern( name ) );

		// Separate preset selection from actions that use the pending copy.
		MenuSite.addLine( this, menu, "-", null );
		String editLabel = "Edit Selected";
		MenuSite.addLine( this, menu, editLabel, event -> editSelectedPattern() );
		editPresetItem = MenuSite.getMyMenuItem( this, menu, editLabel );
		editPresetItem.setEnabled( false );

		// Transform only the pending instance when there is one to place.
		MenuSite.addLine( this, menu, "Rotate Clockwise", event -> {
			if( pendingPattern != null )
				pendingPattern.rotateClockwise();
		} );
		MenuSite.addLine( this, menu, "Flip Horizontally", event -> {
			if( pendingPattern != null )
				pendingPattern.flipHorizontally();
		} );
		MenuSite.addLine( this, menu, "Cancel Placement", event -> cancelPlacement() );
	}

	/**
	 * Pauses the simulation and replaces any pending selection with a fresh copy
	 * of a catalog preset. The crosshair indicates that the next board click
	 * attempts to place the copy.
	 *
	 * @param name exact name of a registered preset
	 */
	private void selectPattern( String name )
	{	Clock.instance().stop();

		// Wait for any generation update before entering placement mode.
		synchronized( boardLock )
		{	pendingPattern = patternCatalog.copyOf( name );
		}

		// Make editing available and show the placement cursor.
		editPresetItem.setEnabled( true );
		setCursor( Cursor.getPredefinedCursor( Cursor.CROSSHAIR_CURSOR ) );
	}

	/**
	 * Opens a modal editor for the current pending pattern. Applying edits
	 * replaces the pending copy and keeps placement active. Dismissing the editor
	 * leaves the prior copy selected. With no pending pattern, this does nothing.
	 */
	private void editSelectedPattern()
	{	if( pendingPattern == null )
			return;

		// Let the dialog own the draft and wait without holding the board lock.
		PresetEditorDialog editor = new PresetEditorDialog( this, pendingPattern, outermostCell.widthInCells() );
		LifePattern editedPattern = editor.showEditor();

		// Publish only an applied result to the placement flow.
		if( editedPattern != null )
		{	synchronized( boardLock )
			{	pendingPattern = editedPattern;
			}
		}
	}

	/**
	 * Discards the pending copy and restores normal board clicks. The board
	 * contents and the catalog prototype are unaffected.
	 */
	private void cancelPlacement()
	{	synchronized( boardLock ) // The clock listener also checks the pending state.
		{	pendingPattern = null;
		}
		// Restore the controls used when no preset is selected.
		editPresetItem.setEnabled( false );
		setCursor( null );
	}

	/**
	 * Attempts to place the pending pattern with its upper-left corner at the
	 * clicked board cell. Success consumes the selection and repaints the board.
	 * If the attempt fails, the selection stays active so the user can try again.
	 *
	 * @param location panel-relative pixel coordinates of the mouse press
	 */
	private void placePendingPattern( Point location )
	{	int pixelsPerCell = getWidth() / outermostCell.widthInCells();
		boolean placed = false;

		// Validate the click before converting pixels to board coordinates.
		if( pixelsPerCell > 0 && contains( location ) )
		{	synchronized( boardLock ) // Keep the board stable through the entire placement.
			{	placed = patternPlacer.place( outermostCell, pendingPattern,
						location.x / pixelsPerCell, location.y / pixelsPerCell );
				if( placed )
				{	Clock.instance().stop(); // Leave the placed pattern ready for an explicit tick.
					pendingPattern = null;
				}
			}
		}

		// Update Swing controls or show the retry message after releasing the lock.
		if( placed )
		{	editPresetItem.setEnabled( false );
			setCursor( null );
			repaint();
		}
		else
		{	JOptionPane.showMessageDialog( this, "The pattern does not fit at that position." );
		}
	}

	/**
	 * Handles a clock tick while no preset is pending. Computes and applies the
	 * next generation under the same lock used for placement and requests a
	 * redraw if the board changed.
	 */
	private void advanceGeneration()
	{	boolean changed;

        synchronized( boardLock )
        {   if( pendingPattern != null )
            {   return;
            }
            changed = board.step();
        }

        if( changed )
        {   refreshNow();
        }
    }

	private void doLoad()
	{	try( FileInputStream in = new FileInputStream(
				Files.userSelected( ".", ".life", "Life File", "Load" ) ) )
		{	Clock.instance().stop();
            synchronized( boardLock )
            {   board.load( in );
            }
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Read Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
		repaint();
	}

	private void doStore()
	{	try( FileOutputStream out = new FileOutputStream(
				Files.userSelected( ".", ".life", "Life File", "Write" ) ) )
		{	Clock.instance().stop();
            synchronized( boardLock )
            {   board.store( out );
            }

        }
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Write Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
	}

	/** Override paint to ask the outermost Neighborhood
	 *  (and any subcells) to draw themselves recursively.
	 *  All knowledge of screen size is also encapsulated.
	 *  (The size is passed into the outermost <code>Cell</code>.)
	 */
    @Override
    public void paint( Graphics g )
    {	Rectangle panelBounds = getBounds();
        panelBounds.x = 0;
        panelBounds.y = 0;

        synchronized( boardLock )
        {   outermostCell.redraw( g, panelBounds, true );
        }
    }

	/** Force a screen refresh by queing a request on
	 *  the Swing event queue. This is an example of the
	 *  Active Object pattern (not covered by the Gang of Four).
	 *  This method is called on every clock tick. Note that
	 *  the redraw() method on a given <code>Cell</code>
	 *  does nothing if the <code>Cell</code> doesn't
	 *  have to be refreshed.
	 */

    private void refreshNow()
    {	SwingUtilities.invokeLater( new Runnable()
        {
            @Override
            public void run()
            {	Graphics g = getGraphics();
                if( g == null )
                    return;

                try
                {   Rectangle panelBounds = getBounds();
                    panelBounds.x = 0;
                    panelBounds.y = 0;

                    synchronized( boardLock )
                    {   outermostCell.redraw( g, panelBounds, false );
                    }
                }
                finally
                {   g.dispose();
                }
            }
        } );
    }
}

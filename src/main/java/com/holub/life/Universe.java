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
 * Coordinates board input, preset actions, boundary selection,
 * and persistence between Swing and the Life model.
 * Creates the composite board and exposes a singleton instance
 * through Universe.instance(). BoardEditor records edits and delegates
 * generation updates and persistence to LifeBoard.
 *
 * @include /etc/license.txt
 */

public class Universe extends JPanel
{
	private final        Neighborhood   outermostCell;
	private final        BoardEditor    editor;
	private final        JMenuItem      undoItem;
	private final        JMenuItem      redoItem;

	private static final String         BOUNDARY_MENU   = "Grid:Boundary";
	private static final Universe       theInstance     = new Universe();

	private final        PatternCatalog patternCatalog  = new PatternCatalog();
	private final        Object 		boardLock       = new Object();

    private 			 LifePattern 	pendingPattern;
	private 			 JMenuItem 		editPresetItem;

    private final        ScoreTracker   scoreTracker    = new ScoreTracker();
    private final        JLabel         statusBar       = new JLabel();

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

	// The private constructor prevents external instantiation.
	private Universe()
	{	// Create the nested Cells that comprise the composite board.

		outermostCell = new Neighborhood
						(	DEFAULT_GRID_SIZE,
							new Neighborhood
							(	DEFAULT_GRID_SIZE,
								new Resident()
							)
						);

        editor = new BoardEditor( outermostCell, scoreTracker );

        statusBar.setOpaque( true );
        statusBar.setBackground( new Color( 245, 245, 245 ) );
        statusBar.setForeground( new Color( 40, 40, 40 ) );
        statusBar.setFont( new Font( Font.SANS_SERIF, Font.PLAIN, 12 ) );
        statusBar.setBorder( BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(
                        0, 0, 1, 0, new Color( 210, 210, 210 ) ),
                BorderFactory.createEmptyBorder( 6, 10, 6, 10 ) ) );

        updateStatusBar();

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
					{	editor.click( e.getPoint(), bounds );
					}
					updateHistoryMenus();
                    updateStatusBar();
                    repaint();
				}
			}
		);

		MenuSite.addLine( this, "Grid", "Clear", event -> {
			synchronized( boardLock )
			{	editor.clear();
			}
			updateHistoryMenus();
            updateStatusBar();
            repaint();
		} );
		MenuSite.addLine( this, "Grid", "Undo", event -> {
			synchronized( boardLock )
			{	editor.undo();
			}
			updateBoundaryMenuLabel();
			updateHistoryMenus();
            updateStatusBar();
            repaint();
		} );
		MenuSite.addLine( this, "Grid", "Redo", event -> {
			synchronized( boardLock )
			{	editor.redo();
			}
			updateBoundaryMenuLabel();
			updateHistoryMenus();
            updateStatusBar();
            repaint();
		} );
		undoItem = MenuSite.getMyMenuItem( this, "Grid", "Undo" );
		redoItem = MenuSite.getMyMenuItem( this, "Grid", "Redo" );

        MenuSite.addLine( this, "Grid", "Load", event -> doLoad() );
        MenuSite.addLine( this, "Grid", "Store", event -> doStore() );

        createPresetMenu(); // Register preset menu

        for( BoundaryPolicy policy : BoundaryPolicy.values() )
            MenuSite.addLine( this, BOUNDARY_MENU, boundaryLabel( policy ),
                    e -> selectBoundary( policy ) );
		updateBoundaryMenuLabel();

        MenuSite.addLine( this, "Grid", "Exit",
                event -> System.exit( 0 ) );

		Clock.instance().addClockListener( new Clock.Listener()
		{
			@Override public void tick()
			{	advanceGeneration();
			}

			@Override public void playbackChanged()
			{	updateHistoryMenus();
			}
		} );
		updateHistoryMenus();
	}

	private void updateHistoryMenus()
	{	boolean paused = !Clock.instance().isRunning();
		undoItem.setEnabled( paused && editor.canUndo() );
		redoItem.setEnabled( paused && editor.canRedo() );
	}

    private void selectBoundary( BoundaryPolicy policy )
    {	Clock.instance().stop();

        synchronized( boardLock )
        {	editor.setBoundaryPolicy( policy );
        }

        updateBoundaryMenuLabel();
        updateHistoryMenus();
        updateStatusBar();
        repaint();
    }

	private static String boundaryLabel( BoundaryPolicy policy )
	{	return switch( policy )
		{	case FIXED -> "Fixed";
			case WRAP_AROUND -> "Wrap Around";
		};
	}

	private void updateBoundaryMenuLabel()
	{	String label = boundaryLabel( editor.boundaryPolicy() );
		MenuSite.getMyMenuItem( this, BOUNDARY_MENU, null ).setText( "Boundary (" + label + ")" );
	}

	/** Returns the singleton created during class initialization. */
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
		PresetEditorDialog dialog = new PresetEditorDialog( this, pendingPattern, outermostCell.widthInCells() );
		LifePattern editedPattern = dialog.showEditor();

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
			{	placed = editor.place( pendingPattern,
						location.x / pixelsPerCell, location.y / pixelsPerCell );
				if( placed )
					pendingPattern = null;
			}
		}

		// Update Swing controls or show the retry message after releasing the lock.
		if( placed )
		{	if( Clock.instance().isRunning() )
				Clock.instance().stop();
			updateHistoryMenus();
            updateStatusBar();
            editPresetItem.setEnabled( false );
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
    {	synchronized( boardLock )
        {	if( pendingPattern != null )
                return;

            editor.step();
        }

        updateHistoryMenus();
        updateStatusBar();
        refreshNow();
    }

	private void doLoad()
	{	try( FileInputStream in = new FileInputStream(
				Files.userSelected( ".", ".life", "Life File", "Load" ) ) )
		{	Clock.instance().stop();
            synchronized( boardLock )
			{	editor.load( in );
				updateHistoryMenus();
            }
		}
		catch( IOException theException )
		{	JOptionPane.showMessageDialog( null, "Read Failed!",
					"The Game of Life", JOptionPane.ERROR_MESSAGE);
		}
        updateStatusBar();
        repaint();
	}

	private void doStore()
	{	try( FileOutputStream out = new FileOutputStream(
				Files.userSelected( ".", ".life", "Life File", "Write" ) ) )
		{	Clock.instance().stop();
            synchronized( boardLock )
			{	editor.store( out );
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
        {	outermostCell.redraw( g, panelBounds, true );
        }
    }

	/**
	 * Queues an incremental redraw on the Swing event thread
	 * after a generation changes the board.
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
                {	Rectangle panelBounds = getBounds();
                    panelBounds.x = 0;
                    panelBounds.y = 0;

                    synchronized( boardLock )
                    {	outermostCell.redraw( g, panelBounds, false );
                    }
                }
                finally
                {	g.dispose();
                }
            }
        } );
    }

    /**
     * Returns the status component displayed above the board.
     */
    JComponent statusBar()
    {	return statusBar;
    }

    /**
     * Updates the status text after a board or scoring action.
     */
    private void updateStatusBar()
    {	String text;

        synchronized( boardLock )
        {	text = String.format(
                "Score: %d  |  Scored GEN: %d  |  Status: %s",
                scoreTracker.getScore(),
                scoreTracker.getScoredGen(),
                scoreTracker.getStatus() );
        }

        statusBar.setText( text );
    }
}

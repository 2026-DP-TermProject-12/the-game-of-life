package com.holub.life;

import com.holub.life.LifePattern.Position;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Provides a modal editor for the currently selected preset. The dialog owns
 * a separate grid of live positions, leaving the input pattern and the game
 * board untouched. Apply creates a new pattern bounded by its live cells.
 * Cancel or closing the window discards the edits. Use the dialog on the
 * Swing event dispatch thread.
 */
final class PresetEditorDialog extends JDialog
{
	private static final int DEFAULT_GRID_SIZE = 9;
	private static final int CELL_SIZE = 28;
	private static final String LIVE_CELL_MARK = "\u25CF";

	private final Set<Position> liveCells = new HashSet<>();
	private final JButton applyButton = new JButton( "Apply" );
	private LifePattern editedPattern;

	/**
	 * Builds the editor grid and centers the current pattern within it. The grid
	 * leaves room to add cells when possible and stays within the board dimensions.
	 *
	 * @param parent component whose window owns the dialog
	 * @param pattern pattern in its current orientation, used to seed the grid
	 * @param boardSize number of columns and rows on the square destination board
	 */
	PresetEditorDialog( Component parent, LifePattern pattern, int boardSize )
	{	super( SwingUtilities.getWindowAncestor( parent ),
			"Edit Preset",
			ModalityType.APPLICATION_MODAL );

		// Reserve room around the pattern without allowing an oversized result.
		int columns = Math.clamp( pattern.width() + 2, DEFAULT_GRID_SIZE, boardSize );
		int rows = Math.clamp( pattern.height() + 2, DEFAULT_GRID_SIZE, boardSize );

		// Seed the editor's own positions, offset so the input pattern starts centered.
		int columnOffset = ( columns - pattern.width() ) / 2;
		int rowOffset = ( rows - pattern.height() ) / 2;
		for( Position position : pattern.liveCells() )
		{   liveCells.add(new Position(position.column() + columnOffset, position.row() + rowOffset));
		}

		// Keep the visible area compact as the editable grid grows.
		JScrollPane scrollPane = new JScrollPane( createGrid( columns, rows ) );
		scrollPane.getViewport().setPreferredSize( new Dimension(
				Math.min( columns, DEFAULT_GRID_SIZE ) * CELL_SIZE,
				Math.min( rows, DEFAULT_GRID_SIZE ) * CELL_SIZE ) );
		add( scrollPane, BorderLayout.CENTER );

		// Only Apply stores a result. Cancel and window close both discard the draft.
		applyButton.setEnabled( !liveCells.isEmpty() );
		applyButton.addActionListener( event -> applyChanges() );
		JButton cancelButton = new JButton( "Cancel" );
		cancelButton.addActionListener( event -> dispose() );
		JPanel buttons = new JPanel();
		buttons.add( applyButton );
		buttons.add( cancelButton );
		add( buttons, BorderLayout.SOUTH );

		// Finish configuring and positioning the modal window before showing it.
		getRootPane().setDefaultButton( applyButton );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		pack();
		setLocationRelativeTo( parent );
	}

	/**
	 * Shows the modal editor and waits for the user to apply or dismiss it.
	 *
	 * @return the edited pattern on Apply, or {@code null} on Cancel or window close
	 */
	LifePattern showEditor()
	{	setVisible( true ); // Modality delays the return until the dialog closes.
		return editedPattern;
	}

	/**
	 * Builds the panel with one toggle button per editor cell.
	 *
	 * @param columns horizontal grid size in cells
	 * @param rows vertical grid size in cells
	 * @return panel containing the cell controls in row-major order
	 */
	private JPanel createGrid( int columns, int rows )
	{	JPanel grid = new JPanel( new GridLayout( rows, columns ) );

		// Size the panel for its full grid so cells remain reachable by scrolling.
		grid.setPreferredSize( new Dimension( columns * CELL_SIZE, rows * CELL_SIZE ) );

		// Match the layout order to the grid's row and column coordinates.
		for( int row = 0; row < rows; row++ )
			for( int column = 0; column < columns; column++ )
				grid.add( createCell( new Position( column, row ) ) );

		return grid;
	}

	/**
	 * Creates a toggle control initialized from the live-cell set.
	 *
	 * @param position zero-based position in the editor grid
	 * @return toggle button that updates that position when activated
	 */
	private JToggleButton createCell( Position position )
	{	boolean alive = liveCells.contains( position );
		JToggleButton cell = new JToggleButton( alive ? LIVE_CELL_MARK : "", alive );

		// Leave space for the live-cell mark and give the control an accessible name.
		cell.setMargin( new Insets( 0, 0, 0, 0 ) );
		cell.getAccessibleContext().setAccessibleName(
				"Row " + (position.row() + 1) + ", column " + (position.column() + 1) );

		// Keep both the editor data and the button label in sync after a click.
		cell.addActionListener( event -> toggleCell( position, cell ) );
		return cell;
	}

	/**
	 * Records a cell's new state and refreshes the visible mark and Apply button.
	 *
	 * @param position position represented by the button
	 * @param cell button whose selected state reflects the user's action
	 */
	private void toggleCell( Position position, JToggleButton cell )
	{	boolean alive = cell.isSelected();

		// Apply the button's new selection state to the draft grid.
		if( alive )
			liveCells.add( position );
		else
			liveCells.remove( position );

		// Refresh the mark and prevent the user from applying an empty pattern.
		cell.setText( alive ? LIVE_CELL_MARK : "" );
		applyButton.setEnabled( !liveCells.isEmpty() );
	}

	/**
	 * Converts the nonempty draft into a new pattern and closes the dialog.
	 * The result contains only the rectangle enclosing all live cells, with its
	 * upper-left corner at {@code (0, 0)} for placement.
	 */
	private void applyChanges()
	{	IntSummaryStatistics columns = liveCells.stream().mapToInt( Position::column ).summaryStatistics();
		IntSummaryStatistics rows = liveCells.stream().mapToInt( Position::row ).summaryStatistics();

		// Remove the editor margin by measuring each cell from the live bounds.
		List<Position> positions = new ArrayList<>( liveCells.size() );
		for( Position position : liveCells )
		{   positions.add(new Position(position.column() - columns.getMin(),
				position.row() - rows.getMin()));
		}

		// Include both endpoints of the bounds, then return the result to the caller.
		editedPattern = new LifePattern( columns.getMax() - columns.getMin() + 1,
				rows.getMax() - rows.getMin() + 1, positions );
		dispose();
	}
}

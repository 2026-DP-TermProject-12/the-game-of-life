package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;

/** Records board edits and generations in the order received from the UI and clock. */
final class BoardEditor
{
	private final Neighborhood board;
	private final LifeBoard generations;
	private final PatternPlacer placer = new PatternPlacer();
	private final UndoManager history = new UndoManager();

	BoardEditor( Neighborhood board )
	{	this.board = board;
		generations = new LifeBoard( board );
	}

	void click( Point point, Rectangle bounds )
	{	executeAndRecord( () -> {
			board.userClicked( point, bounds );
			return true;
		}, false );
	}

	void clear()
	{	if( !board.hasLivingCells() )
			return;

		executeAndRecord( () -> {
			board.clear();
			return true;
		}, false );
	}

	boolean step()
	{	return executeAndRecord( generations::step, true );
	}

	boolean place( LifePattern pattern, int column, int row )
	{	return executeAndRecord( () -> placer.place( board, pattern, column, row ), false );
	}

	void setBoundaryPolicy( BoundaryPolicy policy )
	{	BoundaryPolicy before = generations.boundaryPolicy();
		if( policy == before )
			return;
		generations.setBoundaryPolicy( policy );
		history.addEdit( new BoundaryEdit( before, policy ) );
	}

	BoundaryPolicy boundaryPolicy()
	{	return generations.boundaryPolicy();
	}

	private boolean executeAndRecord( BoardAction action, boolean recordUnchanged )
	{	BoardEdit edit = new BoardEdit();
		boolean changed = edit.execute( action );
		if( changed || recordUnchanged )
			history.addEdit( edit );
		return changed;
	}

	void undo()
	{	if( history.canUndo() )
			history.undo();
	}

	void redo()
	{	if( history.canRedo() )
			history.redo();
	}

	void load( InputStream in ) throws IOException
	{	generations.load( in );
		history.discardAllEdits();
	}

	void store( OutputStream out ) throws IOException
	{	generations.store( out );
	}

	boolean canUndo()
	{	return history.canUndo();
	}

	boolean canRedo()
	{	return history.canRedo();
	}

	@FunctionalInterface
	private interface BoardAction
	{
		/** Apply the action. Return false only when the board is known to be unchanged. */
		boolean apply();
	}

	/** Executes one action and restores its snapshots for later undo and redo. */
	private final class BoardEdit extends AbstractUndoableEdit
	{
		private Storable before;
		private Storable after;

		boolean execute( BoardAction action )
		{	before = board.createMemento();
			boolean needsSnapshot = action.apply();
			after = needsSnapshot ? board.createMemento() : before;
			return needsSnapshot;
		}

		@Override public void undo()
		{	super.undo();
			generations.restore( before );
		}

		@Override public void redo()
		{	super.redo();
			generations.restore( after );
		}
	}

	/** Restores a boundary selection without advancing or copying the board. */
	private final class BoundaryEdit extends AbstractUndoableEdit
	{
		private final BoundaryPolicy before;
		private final BoundaryPolicy after;

		BoundaryEdit( BoundaryPolicy before, BoundaryPolicy after )
		{	this.before = before;
			this.after = after;
		}

		@Override public void undo()
		{	super.undo();
			generations.setBoundaryPolicy( before );
		}

		@Override public void redo()
		{	super.redo();
			generations.setBoundaryPolicy( after );
		}
	}
}

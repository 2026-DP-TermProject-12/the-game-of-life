package com.holub.life;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.awt.*;

/** Records board edits and generations in the order received from the UI and clock. */
final class BoardEditor
{
	private final Neighborhood  board;
	private final LifeBoard     generations;
	private final PatternPlacer placer          = new PatternPlacer();
	private final UndoManager   history         = new UndoManager();
    private final ScoreTracker  scoreTracker;

    BoardEditor( Neighborhood board )
    {	this( board, new ScoreTracker() );
    }

    BoardEditor( Neighborhood board, ScoreTracker scoreTracker )
    {	this.board = board;
        this.scoreTracker = scoreTracker;
        generations = new LifeBoard( board );
    }

    void click( Point point, Rectangle bounds )
    {	executeAndRecord( () -> {
            board.userClicked( point, bounds );
            scoreTracker.reset();
            return true;
        }, false );
    }

    void clear()
    {	if( !board.hasLivingCells() )
        {	scoreTracker.reset();
            return;
        }

        executeAndRecord( () -> {
            board.clear();
            scoreTracker.reset();
            return true;
        }, false );
    }

    boolean step()
    {	return executeAndRecord( () -> {
            boolean changed = generations.step();
            scoreTracker.evaluate( board );
            return changed;
        }, true );
    }

    boolean place( LifePattern pattern, int column, int row )
    {	return executeAndRecord( () -> {
            boolean placed = placer.place( board, pattern, column, row );
            if( placed )
                scoreTracker.reset();
            return placed;
        }, false );
    }

    void setBoundaryPolicy( BoundaryPolicy policy )
    {	BoundaryPolicy before = generations.boundaryPolicy();
        if( policy == before )
            return;

        ScoreTracker.Snapshot scoreBefore = scoreTracker.createSnapshot();

        generations.setBoundaryPolicy( policy );
        scoreTracker.reset();

        history.addEdit( new BoundaryEdit(
                before, policy, scoreBefore, scoreTracker.createSnapshot() ) );
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

	void load( BoardSnapshot snapshot )
	{	generations.load( snapshot );
		scoreTracker.reset();
		history.discardAllEdits();
	}

	BoardSnapshot snapshot()
	{	return generations.snapshot();
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

    /**
     * Restores board and scoring snapshots for undo and redo.
     */
    private final class BoardEdit extends AbstractUndoableEdit
    {
        private Cell.Memento            before;
        private Cell.Memento            after;
        private ScoreTracker.Snapshot   scoreBefore;
        private ScoreTracker.Snapshot   scoreAfter;

        boolean execute( BoardAction action )
        {	before = board.createMemento();
            scoreBefore = scoreTracker.createSnapshot();

            boolean changed = action.apply();

            after = changed ? board.createMemento() : before;
            scoreAfter = scoreTracker.createSnapshot();
            return changed;
        }

        @Override
        public void undo()
        {	super.undo();
            generations.restore( before );
            scoreTracker.restore( scoreBefore );
        }

        @Override
        public void redo()
        {	super.redo();
            generations.restore( after );
            scoreTracker.restore( scoreAfter );
        }
    }

    /**
     * Restores boundary policy and scoring without advancing the board.
     */
    private final class BoundaryEdit extends AbstractUndoableEdit
    {
        private final BoundaryPolicy        before;
        private final BoundaryPolicy        after;
        private final ScoreTracker.Snapshot scoreBefore;
        private final ScoreTracker.Snapshot scoreAfter;

        BoundaryEdit( BoundaryPolicy before, BoundaryPolicy after,
                      ScoreTracker.Snapshot scoreBefore,
                      ScoreTracker.Snapshot scoreAfter )
        {	this.before = before;
            this.after = after;
            this.scoreBefore = scoreBefore;
            this.scoreAfter = scoreAfter;
        }

        @Override
        public void undo()
        {	super.undo();
            generations.setBoundaryPolicy( before );
            scoreTracker.restore( scoreBefore );
        }

        @Override
        public void redo()
        {	super.redo();
            generations.setBoundaryPolicy( after );
            scoreTracker.restore( scoreAfter );
        }
    }
}

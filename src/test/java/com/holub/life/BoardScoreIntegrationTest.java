package com.holub.life;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies scoring together with board editing, history and persistence.
 */
class BoardScoreIntegrationTest
{
    private final Neighborhood board =
            new Neighborhood( 2, new Neighborhood( 4, new Resident() ) );

    private final ScoreTracker tracker = new ScoreTracker();
    private final BoardEditor editor = new BoardEditor( board, tracker );
    private final PatternCatalog catalog = new PatternCatalog();

    private record View(
            Set<Point> liveCells,
            int score,
            int scoredGen,
            ScoreTracker.Status status,
            BoundaryPolicy boundary )
    {
    }

    private Set<Point> liveCells()
    {	Cell.Memento snapshot = (Cell.Memento) board.createMemento();
        Set<Point> result = new HashSet<Point>();

        for( int row = 0; row < board.widthInCells(); row++ )
        {	for( int column = 0; column < board.widthInCells(); column++ )
        {	Point point = new Point( column, row );
            if( snapshot.isAlive( point ) )
                result.add( point );
        }
        }

        return Set.copyOf( result );
    }

    private View view()
    {	return new View(
            liveCells(),
            tracker.getScore(),
            tracker.getScoredGen(),
            tracker.getStatus(),
            editor.boundaryPolicy() );
    }

    private void expectView( View expected )
    {	assertEquals( expected, view() );
    }

    private void expectScore( int scoredGen, ScoreTracker.Status status )
    {	assertAll(
            () -> assertEquals( scoredGen * 10, tracker.getScore() ),
            () -> assertEquals( scoredGen, tracker.getScoredGen() ),
            () -> assertEquals( status, tracker.getStatus() ) );
    }

    private void expectCells( Point... expected )
    {	assertEquals( Set.of( expected ), liveCells() );
    }

    private void click( int column, int row )
    {	int size = board.widthInCells();
        editor.click(
                new Point( column, row ),
                new Rectangle( 0, 0, size, size ) );
    }

    private void placeBlinker()
    {	assertTrue( editor.place( catalog.copyOf( "Blinker" ), 2, 3 ) );
    }

    private byte[] store() throws IOException
    {	ByteArrayOutputStream output = new ByteArrayOutputStream();
        new LifeFileCodec().write( editor.snapshot(), output );
        return output.toByteArray();
    }

    @Test
    void stepUndoRedoRestoresBoardAndScore()
    {	placeBlinker();
        View initial = view();

        assertTrue( editor.step() );
        expectCells(
                new Point( 3, 2 ),
                new Point( 3, 3 ),
                new Point( 3, 4 ) );
        expectScore( 1, ScoreTracker.Status.RUNNING );
        View first = view();

        assertTrue( editor.step() );
        expectCells(
                new Point( 2, 3 ),
                new Point( 3, 3 ),
                new Point( 4, 3 ) );
        expectScore( 2, ScoreTracker.Status.RUNNING );
        View second = view();

        editor.undo();
        expectView( first );

        editor.undo();
        expectView( initial );

        editor.redo();
        expectView( first );

        editor.redo();
        expectView( second );
    }

    @Test
    void unchangedBoardStillRecordsScoreChanges()
    {	assertTrue( editor.place( catalog.copyOf( "Block" ), 2, 2 ) );
        View initial = view();

        assertFalse( editor.step() );
        expectScore( 1, ScoreTracker.Status.RUNNING );
        View first = view();

        assertFalse( editor.step() );
        expectScore( 1, ScoreTracker.Status.STABILIZED );
        View stabilized = view();

        assertEquals( initial.liveCells(), stabilized.liveCells() );

        editor.undo();
        expectView( first );

        editor.undo();
        expectView( initial );

        editor.redo();
        expectView( first );

        editor.redo();
        expectView( stabilized );
    }

    @Test
    void undoRestoresRepetitionHistoryAndFinishedStatus()
    {	placeBlinker();
        editor.step();
        editor.step();
        View running = view();

        editor.step();
        expectScore( 2, ScoreTracker.Status.STABILIZED );
        View stabilized = view();

        // Reusing snapshots must not mutate their saved histories.
        for( int attempt = 0; attempt < 3; attempt++ )
        {	editor.undo();
            expectView( running );

            editor.redo();
            expectView( stabilized );
        }

        editor.undo();
        expectView( running );

        // Replaying the repeated layout must still terminate scoring.
        editor.step();
        expectView( stabilized );
        assertFalse( editor.canRedo() );
    }

    @Test
    void undoRemovesFutureLayoutsBeforeStartingAnotherBranch()
    {	placeBlinker();
        editor.step();
        View first = view();

        editor.step();
        View second = view();

        editor.undo();
        expectView( first );
        assertTrue( editor.canRedo() );

        // The discarded second layout must not remain in scoring history.
        editor.step();
        expectView( second );
        expectScore( 2, ScoreTracker.Status.RUNNING );
        assertFalse( editor.canRedo() );

        editor.step();
        expectScore( 2, ScoreTracker.Status.STABILIZED );
    }

    @Test
    void clickAndClearResetScoresAndCanBeUndone()
    {	placeBlinker();
        editor.step();
        View scored = view();

        click( 7, 7 );
        expectScore( 0, ScoreTracker.Status.RUNNING );
        assertTrue( liveCells().contains( new Point( 7, 7 ) ) );
        View clicked = view();

        editor.undo();
        expectView( scored );

        editor.redo();
        expectView( clicked );

        editor.undo();
        expectView( scored );

        editor.clear();
        expectCells();
        expectScore( 0, ScoreTracker.Status.RUNNING );
        View cleared = view();

        editor.undo();
        expectView( scored );

        editor.redo();
        expectView( cleared );
    }

    @Test
    void overlappingPresetRestoresExistingCellsAndScores()
    {	placeBlinker();
        editor.step();
        View beforePlacement = view();

        assertTrue( editor.place( catalog.copyOf( "Block" ), 3, 3 ) );
        expectCells(
                new Point( 3, 2 ),
                new Point( 3, 3 ),
                new Point( 3, 4 ),
                new Point( 4, 3 ),
                new Point( 4, 4 ) );
        expectScore( 0, ScoreTracker.Status.RUNNING );
        View placed = view();

        editor.undo();
        expectView( beforePlacement );

        editor.redo();
        expectView( placed );

        // The restored placement must evolve and start a new scoring session.
        editor.step();
        expectScore( 1, ScoreTracker.Status.RUNNING );

        editor.undo();
        expectView( placed );
    }

    @Test
    void rejectedPresetPreservesScoresAndRedo()
    {	placeBlinker();
        editor.step();
        View first = view();

        editor.step();
        View second = view();

        editor.undo();
        expectView( first );

        assertFalse( editor.place( catalog.copyOf( "Blinker" ), 7, 7 ) );
        expectView( first );
        assertTrue( editor.canRedo() );

        editor.redo();
        expectView( second );
    }

    @Test
    void boundaryUndoRestoresScoresAndRepetitionHistory()
    {	placeBlinker();
        editor.step();
        View fixed = view();

        editor.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
        assertEquals( BoundaryPolicy.WRAP_AROUND, editor.boundaryPolicy() );
        expectScore( 0, ScoreTracker.Status.RUNNING );
        assertEquals( fixed.liveCells(), liveCells() );
        View wrapped = view();

        editor.undo();
        expectView( fixed );

        editor.redo();
        expectView( wrapped );

        editor.undo();
        expectView( fixed );

        // The old first layout must remain in the restored history.
        editor.step();
        expectScore( 2, ScoreTracker.Status.RUNNING );

        editor.step();
        expectScore( 2, ScoreTracker.Status.STABILIZED );
    }

    @Test
    void selectingSameBoundaryPreservesScoreAndRedo()
    {	placeBlinker();
        editor.step();
        View first = view();

        editor.step();
        View second = view();

        editor.undo();
        expectView( first );

        editor.setBoundaryPolicy( BoundaryPolicy.FIXED );
        expectView( first );
        assertTrue( editor.canRedo() );

        editor.redo();
        expectView( second );
    }

    @Test
    void wrappedGenerationAndScoreAreRestoredTogether()
    {	assertTrue( editor.place( catalog.copyOf( "Blinker" ), 2, 0 ) );
        editor.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
        View initial = view();

        editor.step();
        expectCells(
                new Point( 3, 7 ),
                new Point( 3, 0 ),
                new Point( 3, 1 ) );
        expectScore( 1, ScoreTracker.Status.RUNNING );
        View evolved = view();

        editor.undo();
        expectView( initial );

        editor.redo();
        expectView( evolved );
    }

    @Test
    void maximumScoreStatusSupportsUndoAndRedo()
    {	ScoreTracker limitedTracker = new ScoreTracker( 1 );
        BoardEditor limitedEditor = new BoardEditor( board, limitedTracker );

        assertTrue( limitedEditor.place( catalog.copyOf( "Blinker" ), 2, 3 ) );
        Set<Point> initial = liveCells();

        limitedEditor.step();
        Set<Point> first = liveCells();
        assertEquals( 10, limitedTracker.getScore() );
        assertEquals( 1, limitedTracker.getScoredGen() );
        assertEquals(
                ScoreTracker.Status.MAX_GEN_REACHED,
                limitedTracker.getStatus() );

        limitedEditor.undo();
        assertEquals( initial, liveCells() );
        assertEquals( 0, limitedTracker.getScore() );
        assertEquals( 0, limitedTracker.getScoredGen() );
        assertEquals( ScoreTracker.Status.RUNNING, limitedTracker.getStatus() );

        limitedEditor.redo();
        assertEquals( first, liveCells() );
        assertEquals( 10, limitedTracker.getScore() );
        assertEquals(
                ScoreTracker.Status.MAX_GEN_REACHED,
                limitedTracker.getStatus() );

        // Finishing scoring must not stop board evolution.
        limitedEditor.step();
        assertEquals( initial, liveCells() );
        assertEquals( 10, limitedTracker.getScore() );
        assertEquals( 1, limitedTracker.getScoredGen() );
        assertEquals(
                ScoreTracker.Status.MAX_GEN_REACHED,
                limitedTracker.getStatus() );
    }

    @Test
    void storePreservesScoresAndRedo() throws IOException
    {	placeBlinker();
        editor.step();
        View first = view();

        editor.step();
        View second = view();

        editor.undo();
        expectView( first );
        assertTrue( editor.canRedo() );

        assertTrue( store().length > 0 );
        expectView( first );
        assertTrue( editor.canRedo() );

        editor.redo();
        expectView( second );
    }

    @Test
    void successfulLoadResetsScoringAndClearsHistory() throws IOException
    {	placeBlinker();
        editor.step();
        View saved = view();
        byte[] file = store();

        editor.step();
        editor.step();
        expectScore( 2, ScoreTracker.Status.STABILIZED );

        editor.load( new LifeFileCodec().read( new ByteArrayInputStream( file ), board.widthInCells() ) );
        assertEquals( saved.liveCells(), liveCells() );
        expectScore( 0, ScoreTracker.Status.RUNNING );
        assertFalse( editor.canUndo() );
        assertFalse( editor.canRedo() );

        View loaded = view();
        editor.undo();
        editor.redo();
        expectView( loaded );

        // Loading must discard the previous scoring history as well.
        editor.step();
        expectScore( 1, ScoreTracker.Status.RUNNING );
    }

    @Test
    void failedLoadPreservesBoardScoresAndRedo()
    {	placeBlinker();
        editor.step();
        View first = view();

        editor.step();
        View second = view();

        editor.undo();
        expectView( first );

        assertThrows( IOException.class,
                () -> editor.load( new LifeFileCodec().read( new ByteArrayInputStream( new byte[0] ), board.widthInCells() ) ) );

        expectView( first );
        assertTrue( editor.canRedo() );

        editor.redo();
        expectView( second );
    }

    @Test
    void invalidCoordinateFilePreservesBoardScoresAndRedo() throws IOException
    {	placeBlinker();
        editor.step();
        View first = view();
        editor.step();
        View second = view();
        editor.undo();

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try( ObjectOutputStream output = new ObjectOutputStream( bytes ) )
        {	output.writeObject( java.util.List.of( new Point( 1, 1 ), new Point( 8, 0 ) ) );
        }
        assertThrows( IOException.class, () -> editor.load( new LifeFileCodec().read(
                new ByteArrayInputStream( bytes.toByteArray() ), 8 ) ) );
        expectView( first );
        assertTrue( editor.canRedo() );
        editor.redo();
        expectView( second );
    }

    @Test
    void wrongSnapshotDimensionsPreserveBoardScoresAndRedo()
    {	placeBlinker();
        editor.step();
        View first = view();
        editor.step();
        View second = view();
        editor.undo();

        assertThrows( IllegalArgumentException.class, () ->
                editor.load( new BoardSnapshot( 4, Set.of() ) ) );
        expectView( first );
        assertTrue( editor.canRedo() );
        editor.redo();
        expectView( second );
    }

    @Test
    void clearOnEmptyBoardResetsScoringWithoutRemovingRedo()
    {	editor.step();
        expectCells();
        expectScore( 0, ScoreTracker.Status.EXTINCT );

        click( 7, 7 );
        View clicked = view();

        editor.undo();
        expectCells();
        expectScore( 0, ScoreTracker.Status.EXTINCT );
        assertTrue( editor.canRedo() );

        editor.clear();
        expectCells();
        expectScore( 0, ScoreTracker.Status.RUNNING );
        assertTrue( editor.canRedo() );

        editor.redo();
        expectView( clicked );
    }
}

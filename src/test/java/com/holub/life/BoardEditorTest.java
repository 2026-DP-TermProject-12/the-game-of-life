package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoardEditorTest
{
	private final Neighborhood board = new Neighborhood( 2, new Neighborhood( 4, new Resident() ) );
	private final BoardEditor editor = new BoardEditor( board );

	private void click( int x, int y )
	{	editor.click( new Point( x, y ), new Rectangle( 0, 0, 8, 8 ) );
	}

	private void expectBoard( Point... live )
	{	Cell.Memento snapshot = (Cell.Memento) board.createMemento();
		java.util.List<Point> expected = java.util.Arrays.asList( live );
		for( int y = 0; y < board.widthInCells(); ++y )
			for( int x = 0; x < board.widthInCells(); ++x )
			{	Point cell = new Point( x, y );
				assertEquals( expected.contains( cell ), snapshot.isAlive( cell ), cell.toString() );
			}
	}

	@Test void clickUndoRedo()
	{	click( 1, 5 );
		editor.undo();
		expectBoard();
		editor.redo();
		expectBoard( new Point( 1, 5 ) );
	}

	@Test void repeatedUndoRedoRestoresClearAndClicksInOrder()
	{	click( 1, 5 );
		click( 6, 2 );
		editor.clear();
		expectBoard();
		editor.undo();
		expectBoard( new Point( 1, 5 ), new Point( 6, 2 ) );
		editor.undo();
		expectBoard( new Point( 1, 5 ) );
		editor.redo();
		expectBoard( new Point( 1, 5 ), new Point( 6, 2 ) );
		editor.redo();
		expectBoard();
	}

	@Test void newEditDropsRedo()
	{	click( 0, 1 );
		editor.undo();
		click( 5, 6 );
		editor.redo();
		expectBoard( new Point( 5, 6 ) );
	}

	@Test void emptyHistoryIsSafe()
	{	editor.undo();
		editor.redo();
		expectBoard();
	}

	@Test void playbackPreservesEarlierClicksAndRestoresBoundaryCells()
	{	click( 2, 3 );
		click( 3, 3 );
		click( 4, 3 );
		editor.clear();
		editor.undo();
		editor.step();
		expectBoard( new Point( 3, 2 ), new Point( 3, 3 ), new Point( 3, 4 ) );
		editor.undo();
		expectBoard( new Point( 2, 3 ), new Point( 3, 3 ), new Point( 4, 3 ) );
		editor.undo();
		expectBoard( new Point( 2, 3 ), new Point( 3, 3 ) );
	}

	@Test void undoAndRedoTraverseManualStepsAndEarlierClicks()
	{	click( 2, 3 );
		click( 3, 3 );
		click( 4, 3 );
		Point[] horizontal = { new Point( 2, 3 ), new Point( 3, 3 ), new Point( 4, 3 ) };
		Point[] vertical = { new Point( 3, 2 ), new Point( 3, 3 ), new Point( 3, 4 ) };

		editor.step();
		expectBoard( vertical );
		editor.step();
		expectBoard( horizontal );
		editor.undo();
		expectBoard( vertical );
		editor.undo();
		expectBoard( horizontal );
		editor.undo();
		expectBoard( new Point( 2, 3 ), new Point( 3, 3 ) );
		editor.redo();
		expectBoard( horizontal );
		editor.redo();
		expectBoard( vertical );
		editor.redo();
		expectBoard( horizontal );
	}

	@Test void manualStepAfterUndoReplacesRedo()
	{	click( 2, 3 );
		click( 3, 3 );
		click( 4, 3 );
		editor.clear();
		editor.undo();
		editor.step();
		editor.redo();
		expectBoard( new Point( 3, 2 ), new Point( 3, 3 ), new Point( 3, 4 ) );
	}

	@Test void stableGenerationCountsAsOneManualStep()
	{	click( 2, 2 );
		click( 2, 3 );
		click( 3, 2 );
		click( 3, 3 );
		editor.step();
		editor.undo();
		expectBoard( new Point( 2, 2 ), new Point( 2, 3 ), new Point( 3, 2 ), new Point( 3, 3 ) );
		editor.undo();
		expectBoard( new Point( 2, 2 ), new Point( 2, 3 ), new Point( 3, 2 ) );
	}

	@Test void playbackHistoryIsLimitedToTheLatestHundredActions()
	{	click( 2, 3 );
		click( 3, 3 );
		click( 4, 3 );
		for( int i = 0; i < 101; ++i )
			editor.step();
		for( int i = 0; i < 100; ++i )
			editor.undo();
		expectBoard( new Point( 3, 2 ), new Point( 3, 3 ), new Point( 3, 4 ) );
		editor.undo();
		expectBoard( new Point( 3, 2 ), new Point( 3, 3 ), new Point( 3, 4 ) );
	}

	@Test void storeKeepsHistoryAndLoadStartsNewHistory() throws IOException
	{	click( 6, 1 );
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		editor.store( file );
		editor.undo();
		expectBoard();
		editor.load( new ByteArrayInputStream( file.toByteArray() ) );
		editor.undo();
		editor.redo();
		expectBoard( new Point( 6, 1 ) );
	}

	@Test void loadsOriginalLifeFormatWithDuplicateCoordinates() throws IOException
	{	LinkedList<Point> legacy = new LinkedList<>();
		legacy.add( new Point( 2, 7 ) );
		legacy.add( new Point( 2, 7 ) );
		ByteArrayOutputStream file = new ByteArrayOutputStream();
		try( ObjectOutputStream out = new ObjectOutputStream( file ) )
		{	out.writeObject( legacy );
		}
		click( 4, 3 );
		editor.load( new ByteArrayInputStream( file.toByteArray() ) );
		editor.undo();
		expectBoard( new Point( 2, 7 ) );
	}

	@Test void unreadableFilePreservesBoardAndUndo()
	{	click( 1, 5 );
		assertThrows( IOException.class, () -> editor.load( new ByteArrayInputStream( new byte[0] ) ) );
		expectBoard( new Point( 1, 5 ) );
		editor.undo();
		expectBoard();
	}

	@Test void presetPlacementRestoresOverlappingCellsInOneUndo()
	{	click( 2, 3 );
		LifePattern pattern = new PatternCatalog().copyOf( "Blinker" );
		assertTrue( editor.place( pattern, 2, 3 ) );
		expectBoard( new Point( 2, 3 ), new Point( 3, 3 ), new Point( 4, 3 ) );
		editor.undo();
		expectBoard( new Point( 2, 3 ) );
		assertFalse( editor.place( pattern, 7, 3 ) );
		assertTrue( editor.canRedo() );
		editor.redo();
		expectBoard( new Point( 2, 3 ), new Point( 3, 3 ), new Point( 4, 3 ) );
	}

	@Test void restoredPresetEvolvesUsingCurrentBoundary()
	{	editor.place( new PatternCatalog().copyOf( "Blinker" ), 2, 0 );
		editor.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		editor.step();
		expectBoard( new Point( 3, 7 ), new Point( 3, 0 ), new Point( 3, 1 ) );
		editor.undo();
		editor.step();
		expectBoard( new Point( 3, 7 ), new Point( 3, 0 ), new Point( 3, 1 ) );
		editor.undo();
		editor.setBoundaryPolicy( BoundaryPolicy.FIXED );
		editor.step();
		expectBoard( new Point( 3, 0 ), new Point( 3, 1 ) );
	}

	@Test void undoRedoFollowsBoundarySelectionAndActualGenerations()
	{	Point[] pair = { new Point( 3, 0 ), new Point( 3, 1 ) };
		editor.place( new PatternCatalog().copyOf( "Blinker" ), 2, 0 );
		editor.step();
		expectBoard( pair );
		editor.step();
		expectBoard();
		editor.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		expectBoard();
		editor.undo();
		assertEquals( BoundaryPolicy.FIXED, editor.boundaryPolicy() );
		expectBoard();
		editor.undo();
		expectBoard( pair );
		editor.redo();
		expectBoard();
		editor.redo();
		assertEquals( BoundaryPolicy.WRAP_AROUND, editor.boundaryPolicy() );
		expectBoard();
	}

	@Test void onlyAnActualBoundaryChangeReplacesRedo()
	{	editor.place( new PatternCatalog().copyOf( "Blinker" ), 2, 0 );
		editor.step();
		editor.undo();
		editor.setBoundaryPolicy( BoundaryPolicy.FIXED );
		assertTrue( editor.canRedo() );
		editor.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		assertFalse( editor.canRedo() );
		editor.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		editor.undo();
		assertEquals( BoundaryPolicy.FIXED, editor.boundaryPolicy() );
		editor.redo();
		editor.step();
		expectBoard( new Point( 3, 7 ), new Point( 3, 0 ), new Point( 3, 1 ) );
	}

	@Test void clearOnEmptyBoardPreservesUndoAndRedo()
	{	editor.clear();
		assertFalse( editor.canUndo() );

		editor.step();
		click( 7, 7 );
		editor.undo();
		editor.clear();
		assertTrue( editor.canRedo() );
		editor.redo();
		expectBoard( new Point( 7, 7 ) );

		editor.clear();
		expectBoard();
		editor.clear();
		editor.undo();
		expectBoard( new Point( 7, 7 ) );
		editor.redo();
		expectBoard();
	}
}

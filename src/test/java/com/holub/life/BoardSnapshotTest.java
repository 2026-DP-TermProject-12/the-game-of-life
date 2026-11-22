package com.holub.life;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class BoardSnapshotTest
{
	@Test
	void ownsAnImmutableCopyOfItsCoordinates()
	{	Set<BoardSnapshot.Position> cells = new HashSet<>();
		cells.add( new BoardSnapshot.Position( 1, 2 ) );
		BoardSnapshot snapshot = new BoardSnapshot( 4, cells );
		cells.clear();
		assertEquals( Set.of( new BoardSnapshot.Position( 1, 2 ) ), snapshot.liveCells() );
		assertThrows( UnsupportedOperationException.class, () -> snapshot.liveCells().clear() );
	}

	@ParameterizedTest
	@CsvSource( { "-1,0", "0,-1", "4,0", "0,4" } )
	void rejectsCoordinatesOutsideTheBoard( int column, int row )
	{	assertThrows( IllegalArgumentException.class, () -> new BoardSnapshot( 4,
				Set.of( new BoardSnapshot.Position( column, row ) ) ) );
	}

	@Test
	void rejectsInvalidDimensionsAndNullCoordinates()
	{	assertThrows( IllegalArgumentException.class, () -> new BoardSnapshot( 0, Set.of() ) );
		assertThrows( IllegalArgumentException.class, () -> new BoardSnapshot( -1, Set.of() ) );
		Set<BoardSnapshot.Position> cells = new HashSet<>();
		cells.add( null );
		assertThrows( NullPointerException.class, () -> new BoardSnapshot( 4, cells ) );
	}

	@Test
	void capturesNestedLogicalCoordinatesWithoutChangingActiveEdges()
	{	Neighborhood root = new Neighborhood( 2, new Neighborhood( 4, new Resident() ) );
		root.userClicked( new Point( 1, 4 ), new Rectangle( 0, 0, 8, 8 ) );
		Direction before = new Direction( root.isDisruptiveTo() );
		BoardSnapshot snapshot = root.snapshot();
		assertEquals( Set.of( new BoardSnapshot.Position( 1, 4 ) ), snapshot.liveCells() );
		assertTrue( before.equals( root.isDisruptiveTo() ) );
		root.clear();
		assertEquals( Set.of( new BoardSnapshot.Position( 1, 4 ) ), snapshot.liveCells() );
	}
}

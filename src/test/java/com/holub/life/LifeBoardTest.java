package com.holub.life;

import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifeBoardTest
{
	private static final int SIZE = 64;
	private final Neighborhood root = new Neighborhood( 8, new Neighborhood( 8, new Resident() ) );
	private final LifeBoard board = new LifeBoard( root );

	@Test
	void fixedBoundaryIsDefaultAndDoesNotConnectOppositeEdges()
	{	seed( cell( 30, 63 ), cell( 30, 0 ), cell( 30, 1 ) );

		assertTrue( board.step() );
		assertLiving();
		assertFalse( board.step() );
	}

	@Test
	void wrapConnectsLeftAndRightEdgesAcrossGenerations()
	{	board.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		seed( cell( 30, 63 ), cell( 30, 0 ), cell( 30, 1 ) );

		assertTrue( board.step() );
		assertLiving( cell( 29, 0 ), cell( 30, 0 ), cell( 31, 0 ) );
		assertTrue( board.step() );
		assertLiving( cell( 30, 63 ), cell( 30, 0 ), cell( 30, 1 ) );
	}

	@Test
	void wrapConnectsTopAndBottomEdges()
	{	board.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		seed( cell( 63, 30 ), cell( 0, 30 ), cell( 1, 30 ) );

		assertTrue( board.step() );
		assertLiving( cell( 0, 29 ), cell( 0, 30 ), cell( 0, 31 ) );
	}

	@Test
	void wrapCountsTheDiagonallyOppositeCorner()
	{	board.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		seed( cell( 0, 63 ), cell( 63, 0 ), cell( 63, 63 ) );

		assertTrue( board.step() );
		assertLiving( cell( 0, 0 ), cell( 0, 63 ), cell( 63, 0 ), cell( 63, 63 ) );
		assertTrue( root.isDisruptiveTo().the( Direction.NORTHWEST ) );
		assertFalse( board.step() );
		assertTrue( root.isDisruptiveTo().equals( Direction.NONE ) );
	}

	@Test
	void switchingToWrapRechecksStableBlocksAndPreservesTheCurrentGeneration()
	{	Point[] blocks = {
				cell( 30, 62 ), cell( 30, 63 ), cell( 31, 62 ), cell( 31, 63 ),
				cell( 30, 0 ), cell( 30, 1 ), cell( 31, 0 ), cell( 31, 1 )
		};
		seed( blocks );
		// A distant death applies a transition and clears the edge flags.
		seed( cell( 26, 26 ) );
		assertTrue( board.step() );
		assertFalse( board.step() );

		board.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		assertLiving( blocks );
		assertTrue( board.step() );
		assertLiving(
				cell( 29, 63 ), cell( 29, 0 ), cell( 30, 62 ), cell( 30, 1 ),
				cell( 31, 62 ), cell( 31, 1 ), cell( 32, 63 ), cell( 32, 0 ) );
	}

	@Test
	void switchingBackToFixedRechecksAStableWrappedBlock()
	{	Point[] wrappedBlock = {cell( 30, 63 ), cell( 30, 0 ), cell( 31, 63 ), cell( 31, 0 )};
		board.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		seed( wrappedBlock );
		seed( cell( 26, 26 ) );
		assertTrue( board.step() );
		assertFalse( board.step() );

		board.setBoundaryPolicy( BoundaryPolicy.FIXED );
		assertLiving( wrappedBlock );
		assertTrue( board.step() );
		assertLiving();
	}

	@Test
	void legacySaveRecalculatesBirthAcrossAnInactiveBlockBoundary() throws IOException
	{	seed( cell( 0, 0 ) );
		loadLegacy( cell( 10, 7 ), cell( 11, 7 ), cell( 12, 7 ) );

		assertLiving( cell( 10, 7 ), cell( 11, 7 ), cell( 12, 7 ) );
		assertTrue( board.step() );
		assertLiving( cell( 11, 6 ), cell( 11, 7 ), cell( 11, 8 ) );
		assertTrue( board.step() );
		assertLiving( cell( 10, 7 ), cell( 11, 7 ), cell( 12, 7 ) );
	}

	@Test
	void saveUsesLegacyCoordinatesAndLoadUsesTheCurrentlySelectedPolicy() throws Exception
	{	Point a = cell( 30, 63 ), b = cell( 30, 0 ), c = cell( 30, 1 );
		board.setBoundaryPolicy( BoundaryPolicy.WRAP_AROUND );
		seed( a, b, c );
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		board.store( saved );

		try( ObjectInputStream input = new ObjectInputStream( new ByteArrayInputStream( saved.toByteArray() ) ) )
		{
			assertEquals( Set.of( a, b, c ), Set.copyOf( ( Collection<?> ) input.readObject() ) );
		}
		board.setBoundaryPolicy( BoundaryPolicy.FIXED );
		board.load( new ByteArrayInputStream( saved.toByteArray() ) );
		assertTrue( board.step() );
		assertLiving();
	}

	@Test
	void failedReadLeavesTheExistingPlacementAvailable()
	{	seed( cell( 20, 19 ), cell( 20, 20 ), cell( 20, 21 ) );

		assertThrows( IOException.class, () -> board.load( new ByteArrayInputStream( new byte[0] ) ) );
		assertLiving( cell( 20, 19 ), cell( 20, 20 ), cell( 20, 21 ) );
		assertTrue( board.step() );
		assertLiving( cell( 19, 20 ), cell( 20, 20 ), cell( 21, 20 ) );
	}

	@Test
	void aRealChangeActivatesTheNeighboringBlockOnTheNextStep() throws IOException
	{	// This glider crosses from the block ending at column 7 into column 8.
		loadLegacy( cell( 9, 4 ), cell( 10, 5 ), cell( 11, 3 ), cell( 11, 4 ), cell( 11, 5 ) );

		for( int generation = 0; generation < 8; generation++ )
		{
			assertTrue( board.step() );
		}
		assertLiving( cell( 11, 6 ), cell( 12, 7 ), cell( 13, 5 ), cell( 13, 6 ), cell( 13, 7 ) );
		for( int generation = 0; generation < 4; generation++ )
		{
			assertTrue( board.step() );
		}
		assertLiving( cell( 12, 7 ), cell( 13, 8 ), cell( 14, 6 ), cell( 14, 7 ), cell( 14, 8 ) );
	}

	private void seed( Point... cells )
	{	Rectangle bounds = new Rectangle( 0, 0, SIZE, SIZE );
		for( Point cell : cells )
		{
			root.userClicked( cell, bounds );
		}
	}

	private void loadLegacy( Point... cells ) throws IOException
	{	ByteArrayOutputStream saved = new ByteArrayOutputStream();
		try( ObjectOutputStream output = new ObjectOutputStream( saved ) )
		{
			output.writeObject( new LinkedList<>( Arrays.asList( cells ) ) );
		}
		board.load( new ByteArrayInputStream( saved.toByteArray() ) );
	}

	private void assertLiving( Point... expected )
	{	Cell.Memento snapshot = ( Cell.Memento ) root.createMemento();
		Set<Point> living = Set.of( expected );
		for( int row = 0; row < SIZE; row++ )
		{
			for( int column = 0; column < SIZE; column++ )
			{
				Point point = cell( row, column );
				assertEquals( living.contains( point ), snapshot.isAlive( point ),
						() -> "row " + point.y + ", column " + point.x );
			}
		}
	}

	private static Point cell( int row, int column )
	{	return new Point( column, row );
	}
}

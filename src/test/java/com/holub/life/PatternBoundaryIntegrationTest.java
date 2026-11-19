package com.holub.life;

import com.holub.life.LifePattern.Position;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Exercises preset placement and boundary-aware generations on the same board. */
class PatternBoundaryIntegrationTest
{
	private static final int SIZE = 64;
	private final Neighborhood root = new Neighborhood( 8, new Neighborhood( 8, new Resident() ) );
	private final LifeBoard board = new LifeBoard( root );
	private final PatternCatalog catalog = new PatternCatalog();
	private final PatternPlacer placer = new PatternPlacer();

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void centralBlinkerOscillatesAcrossInternalNeighborhoods( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		place( "Blinker", 6, 7 );
		assertLiving( position( 6, 7 ), position( 7, 7 ), position( 8, 7 ) );
		assertTrue( board.step() );
		assertLiving( position( 7, 6 ), position( 7, 7 ), position( 7, 8 ) );
		assertTrue( board.step() );
		assertLiving( position( 6, 7 ), position( 7, 7 ), position( 8, 7 ) );
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void rotatedBlinkerUsesSelectedLeftRightBoundary( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		LifePattern pattern = catalog.copyOf( "Blinker" );
		pattern.rotateClockwise();
		assertTrue( placer.place( root, pattern, 0, 30 ) );
		assertLiving( position( 0, 30 ), position( 0, 31 ), position( 0, 32 ) );

		assertTrue( board.step() );
		if( policy == BoundaryPolicy.WRAP_AROUND )
		{	assertLiving( position( 63, 31 ), position( 0, 31 ), position( 1, 31 ) );
			assertTrue( board.step() );
			assertLiving( position( 0, 30 ), position( 0, 31 ), position( 0, 32 ) );
		}
		else
		{	assertLiving( position( 0, 31 ), position( 1, 31 ) );
			assertTrue( board.step() );
			assertLiving();
		}
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void blinkerUsesSelectedTopBottomBoundary( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		place( "Blinker", 30, 0 );
		assertTrue( board.step() );
		if( policy == BoundaryPolicy.WRAP_AROUND )
		{	assertLiving( position( 31, 63 ), position( 31, 0 ), position( 31, 1 ) );
			assertTrue( board.step() );
			assertLiving( position( 30, 0 ), position( 31, 0 ), position( 32, 0 ) );
		}
		else
		{	assertLiving( position( 31, 0 ), position( 31, 1 ) );
			assertTrue( board.step() );
			assertLiving();
		}
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void outOfBoundsPresetsNeverPartiallyPlaceOrWrap( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		place( "Block", 20, 20 );
		Set<Position> before = liveCells( root );
		LifePattern pattern = catalog.copyOf( "Glider" );
		int[][] origins = { { -1, 0 }, { 0, -1 }, { 62, 0 }, { 0, 62 }, { 62, 62 } };
		for( int[] origin : origins )
		{	assertFalse( placer.place( root, pattern, origin[0], origin[1] ) );
			assertEquals( before, liveCells( root ), "Rejected placement must preserve the whole board" );
		}
		assertFalse( board.step() );
		assertEquals( before, liveCells( root ) );
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void blocksFitExactlyAtEveryCornerAndRemainStable( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		for( int column : new int[] { 0, SIZE - 2 } )
		{	for( int row : new int[] { 0, SIZE - 2 } )
			{	root.clear();
				place( "Block", column, row );
				assertLiving( position( column, row ), position( column + 1, row ),
						position( column, row + 1 ), position( column + 1, row + 1 ) );
				assertFalse( board.step() );
				assertLiving( position( column, row ), position( column + 1, row ),
						position( column, row + 1 ), position( column + 1, row + 1 ) );
			}
		}
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void overlappingPlacementsPreserveExistingCellsAndStillAdvance( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		place( "Block", 20, 20 );
		place( "Blinker", 6, 7 );
		place( "Blinker", 6, 7 );
		assertLiving( position( 20, 20 ), position( 21, 20 ), position( 20, 21 ), position( 21, 21 ),
				position( 6, 7 ), position( 7, 7 ), position( 8, 7 ) );
		assertTrue( board.step() );
		assertLiving( position( 20, 20 ), position( 21, 20 ), position( 20, 21 ), position( 21, 21 ),
				position( 7, 6 ), position( 7, 7 ), position( 7, 8 ) );
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void switchingPolicyAfterPlacementPreservesCellsAndAffectsNextGeneration( BoundaryPolicy target )
	{	board.setBoundaryPolicy( target == BoundaryPolicy.FIXED
				? BoundaryPolicy.WRAP_AROUND : BoundaryPolicy.FIXED );
		place( "Blinker", 30, 0 );
		Set<Position> before = liveCells( root );
		board.setBoundaryPolicy( target );
		assertEquals( before, liveCells( root ) );
		assertTrue( board.step() );
		if( target == BoundaryPolicy.WRAP_AROUND )
			assertLiving( position( 31, 63 ), position( 31, 0 ), position( 31, 1 ) );
		else
			assertLiving( position( 31, 0 ), position( 31, 1 ) );
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void reflectedGliderMovesInItsReflectedDirection( BoundaryPolicy policy )
	{	board.setBoundaryPolicy( policy );
		LifePattern pattern = catalog.copyOf( "Glider" );
		pattern.flipHorizontally();
		assertTrue( placer.place( root, pattern, 8, 6 ) );
		assertLiving( position( 9, 6 ), position( 8, 7 ), position( 10, 8 ),
				position( 9, 8 ), position( 8, 8 ) );
		for( int generation = 0; generation < 4; generation++ )
			assertTrue( board.step() );
		assertLiving( position( 8, 7 ), position( 7, 8 ), position( 9, 9 ),
				position( 8, 9 ), position( 7, 9 ) );
	}

	@ParameterizedTest
	@EnumSource( BoundaryPolicy.class )
	void storedPresetRetainsItsEvolutionAfterLoading( BoundaryPolicy policy ) throws IOException
	{	board.setBoundaryPolicy( policy );
		place( "Blinker", 30, 0 );
		ByteArrayOutputStream saved = new ByteArrayOutputStream();
		board.store( saved );

		Neighborhood loadedRoot = new Neighborhood( 8, new Neighborhood( 8, new Resident() ) );
		LifeBoard loadedBoard = new LifeBoard( loadedRoot );
		// The legacy file stores cells only. Configure the policy independently.
		loadedBoard.setBoundaryPolicy( policy );
		loadedBoard.load( new ByteArrayInputStream( saved.toByteArray() ) );
		assertEquals( liveCells( root ), liveCells( loadedRoot ) );
		for( int generation = 0; generation < 4; generation++ )
		{	assertEquals( board.step(), loadedBoard.step() );
			assertEquals( liveCells( root ), liveCells( loadedRoot ),
					"Loaded and original generations must match" );
		}
	}

	private void place( String name, int column, int row )
	{	assertTrue( placer.place( root, catalog.copyOf( name ), column, row ) );
	}

	private void assertLiving( Position... expected )
	{	assertEquals( Set.of( expected ), liveCells( root ) );
	}

	private static Position position( int column, int row )
	{	return new Position( column, row );
	}

	private static Set<Position> liveCells( Neighborhood source )
	{	Cell.Memento snapshot = (Cell.Memento) source.createMemento();
		Set<Position> cells = new HashSet<>();
		for( int row = 0; row < source.widthInCells(); row++ )
		{	for( int column = 0; column < source.widthInCells(); column++ )
			{	if( snapshot.isAlive( new Point( column, row ) ) )
					cells.add( position( column, row ) );
			}
		}
		return cells;
	}
}

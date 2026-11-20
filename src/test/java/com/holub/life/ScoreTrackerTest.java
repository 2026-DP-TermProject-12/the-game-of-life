package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ScoreVisitor and ScoreTracker without UI dependencies.
 */
public class ScoreTrackerTest
{
	private void setAlive( Neighborhood board, int row, int col )
	{	int size = board.widthInCells();
		Rectangle surface = new Rectangle( 0, 0, size, size );
		board.userClicked( new Point( col, row ), surface );
	}

	private void nextGeneration( Neighborhood board )
	{	board.figureNextState(
			Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
			Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY );
		board.transition();
	}

	@Test
	void testExtinctionStopsScoring()
	{	Neighborhood board = new Neighborhood( 4, new Resident() );
		setAlive( board, 1, 1 );

		ScoreTracker tracker = new ScoreTracker();
		nextGeneration( board );
		tracker.evaluate( board );

		assertEquals( ScoreTracker.Status.EXTINCT, tracker.getStatus() );
		assertEquals( 0, tracker.getScore() );
		assertEquals( 0, tracker.getScoredGen() );
	}

	@Test
	void testStillLifeBlockStabilizes()
	{	Neighborhood board = new Neighborhood( 4, new Resident() );
		setAlive( board, 1, 1 );
		setAlive( board, 1, 2 );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );

		ScoreTracker tracker = new ScoreTracker();
		nextGeneration( board );
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );

		nextGeneration( board );
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.STABILIZED, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );
		assertEquals( 1, tracker.getScoredGen() );
	}

	@Test
	void testBlinkerOscillatorStabilizes()
	{	Neighborhood board = new Neighborhood( 5, new Resident() );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );
		setAlive( board, 2, 3 );

		ScoreTracker tracker = new ScoreTracker();
		nextGeneration( board );
		tracker.evaluate( board ); // Vertical (1st scored)
		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );

		nextGeneration( board );
		tracker.evaluate( board ); // Horizontal (2nd scored)
		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 20, tracker.getScore() );

		nextGeneration( board );
		tracker.evaluate( board ); // Vertical again -> Stabilized
		assertEquals( ScoreTracker.Status.STABILIZED, tracker.getStatus() );
		assertEquals( 20, tracker.getScore() );
		assertEquals( 2, tracker.getScoredGen() );
	}

	@Test
	void testMaxGenerationsCap()
	{	Neighborhood board = new Neighborhood( 5, new Resident() );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );
		setAlive( board, 2, 3 );

		ScoreTracker tracker = new ScoreTracker( 1 );
		nextGeneration( board );
		tracker.evaluate( board );

		assertEquals( ScoreTracker.Status.MAX_GENERATIONS_REACHED, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );
		assertEquals( 1, tracker.getScoredGen() );

		nextGeneration( board );
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.MAX_GENERATIONS_REACHED, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );
	}

	@Test
	void testDifferentLayoutsDoNotCollide()
	{	// Layouts that previously collided under `hash * 31 + cellIndex` (= 16403):
		// Board A: 1-based indices (1, 35) -> (0,0) and (5,4)
		// Board B: 1-based indices (2, 4)  -> (0,1) and (0,3)
		Neighborhood boardA = new Neighborhood( 6, new Resident() );
		setAlive( boardA, 0, 0 );
		setAlive( boardA, 5, 4 );

		Neighborhood boardB = new Neighborhood( 6, new Resident() );
		setAlive( boardB, 0, 1 );
		setAlive( boardB, 0, 3 );

		ScoreTracker tracker = new ScoreTracker();
		tracker.evaluate( boardA );
		tracker.evaluate( boardB );

		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 20, tracker.getScore() );
		assertEquals( 2, tracker.getScoredGen() );
	}

	@Test
	void testMovingPatternDoesNotFalselyStabilize()
	{	// Glider keeps the same live count (5) and repeats its relative shape every 4 ticks,
		// but its board layout changes every generation and must not falsely stabilize.
		Neighborhood board = new Neighborhood( 8, new Resident() );
		setAlive( board, 0, 1 );
		setAlive( board, 1, 2 );
		setAlive( board, 2, 0 );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );

		ScoreTracker tracker = new ScoreTracker();
		for( int i = 0; i < 5; i++ )
		{	nextGeneration( board );
			tracker.evaluate( board );
		}

		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 50, tracker.getScore() );
		assertEquals( 5, tracker.getScoredGen() );
	}
}
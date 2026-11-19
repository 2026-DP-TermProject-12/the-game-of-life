package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for ScoreVisitor and ScoreTracker without UI dependencies.
 */
public class ScoreTrackerTest
{
	private void setAlive( Neighborhood board, int row, int col )
	{	Resident cell = (Resident) board.getGrid()[row][col];
		if( !cell.isAlive() )
		{	cell.userClicked( null, null );
		}
		board.markActive();
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
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );

		nextGeneration( board );
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.EXTINCT, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );
	}

	@Test
	void testStillLifeBlockStabilizes()
	{	Neighborhood board = new Neighborhood( 4, new Resident() );
		setAlive( board, 1, 1 );
		setAlive( board, 1, 2 );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );

		ScoreTracker tracker = new ScoreTracker();
		tracker.evaluate( board );
		nextGeneration( board );
		tracker.evaluate( board );

		assertEquals( ScoreTracker.Status.STABILIZED, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );
	}

	@Test
	void testBlinkerOscillatorStabilizes()
	{	Neighborhood board = new Neighborhood( 5, new Resident() );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );
		setAlive( board, 2, 3 );

		ScoreTracker tracker = new ScoreTracker();
		tracker.evaluate( board );
		nextGeneration( board );
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.RUNNING, tracker.getStatus() );
		assertEquals( 20, tracker.getScore() );

		nextGeneration( board );
		tracker.evaluate( board );
		assertEquals( ScoreTracker.Status.STABILIZED, tracker.getStatus() );
		assertEquals( 20, tracker.getScore() );
	}

	@Test
	void testMaxGenerationsCap()
	{	Neighborhood board = new Neighborhood( 5, new Resident() );
		setAlive( board, 2, 1 );
		setAlive( board, 2, 2 );
		setAlive( board, 2, 3 );

		ScoreTracker tracker = new ScoreTracker( 1 );
		tracker.evaluate( board );
		nextGeneration( board );
		tracker.evaluate( board );

		assertEquals( ScoreTracker.Status.MAX_GENERATIONS_REACHED, tracker.getStatus() );
		assertEquals( 10, tracker.getScore() );
	}
}
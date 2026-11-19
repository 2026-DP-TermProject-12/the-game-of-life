package com.holub.life;

import java.util.HashSet;
import java.util.Set;

/**
 * Tracks survival generations and calculates scores using ScoreVisitor.
 * Stops scoring upon extinction, stabilization, repetition, or max generation cap.
 */
public class ScoreTracker
{
	public enum Status
	{	RUNNING,
		EXTINCT,
		STABILIZED,
		MAX_GENERATIONS_REACHED
	}

	private static final int DEFAULT_MAX_GENERATIONS = 500;
	private static final int POINTS_PER_GENERATION   = 10;

	private final int       maxGenerations;
	private final Set<Long> historyHashes = new HashSet<Long>();

	private int    generation = 0;
	private int    score      = 0;
	private Status status     = Status.RUNNING;

	public ScoreTracker()
	{	this( DEFAULT_MAX_GENERATIONS );
	}

	public ScoreTracker( int maxGenerations )
	{	this.maxGenerations = maxGenerations;
	}

	/**
	 * Evaluates the current board state and updates score/status.
	 */
	public void evaluate( Cell rootCell )
	{	if( status != Status.RUNNING )
		{	return;
		}

		if( generation >= maxGenerations )
		{	status = Status.MAX_GENERATIONS_REACHED;
			return;
		}

		ScoreVisitor visitor = new ScoreVisitor();
		rootCell.accept( visitor );

		if( visitor.getLiveCount() == 0 )
		{	status = Status.EXTINCT;
			return;
		}

		long currentHash = visitor.getBoardHash();
		if( !historyHashes.add( currentHash ) )
		{	status = Status.STABILIZED;
			return;
		}

		generation++;
		score += POINTS_PER_GENERATION;
	}

	public void reset()
	{	historyHashes.clear();
		generation = 0;
		score      = 0;
		status     = Status.RUNNING;
	}

	public int getScore()
	{	return score;
	}

	public int getGeneration()
	{	return generation;
	}

	public Status getStatus()
	{	return status;
	}

	public boolean isFinished()
	{	return status != Status.RUNNING;
	}
}
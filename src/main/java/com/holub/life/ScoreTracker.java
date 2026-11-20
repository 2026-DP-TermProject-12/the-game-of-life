package com.holub.life;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

/**
 * Tracks scored survival generations and calculates scores using ScoreVisitor.
 * Stops scoring upon extinction, stabilization, repetition, or max GEN cap.
 */
public class ScoreTracker
{
	public enum Status
	{	RUNNING,
		EXTINCT,
		STABILIZED,
		MAX_GEN_REACHED
	}

	private static final int DEFAULT_MAX_GEN = 500;
	private static final int POINTS_PER_GEN  = 10;

	private final int         maxGen;
	private final Set<BitSet> historyStates = new HashSet<BitSet>();

	private int    scoredGen = 0;
	private int    score     = 0;
	private Status status    = Status.RUNNING;

	public ScoreTracker()
	{	this( DEFAULT_MAX_GEN );
	}

	public ScoreTracker( int maxGen )
	{	if( maxGen <= 0 )
		{	throw new IllegalArgumentException( "maxGen must be positive" );
		}
		this.maxGen = maxGen;
	}

	/**
	 * Evaluates the current board state after a tick and updates score/status.
	 */
	public void evaluate( Cell rootCell )
	{	if( status != Status.RUNNING )
		{	return;
		}

		ScoreVisitor visitor = new ScoreVisitor();
		rootCell.accept( visitor );

		if( visitor.getLiveCount() == 0 )
		{	status = Status.EXTINCT;
			return;
		}

		BitSet currentState = visitor.getBoardState();
		if( !historyStates.add( currentState ) )
		{	status = Status.STABILIZED;
			return;
		}

		scoredGen++;
		score += POINTS_PER_GEN;

		if( scoredGen >= maxGen )
		{	status = Status.MAX_GEN_REACHED;
		}
	}

	public void reset()
	{	historyStates.clear();
		scoredGen = 0;
		score     = 0;
		status    = Status.RUNNING;
	}

	public int getScore()
	{	return score;
	}

	public int getScoredGen()
	{	return scoredGen;
	}

	public Status getStatus()
	{	return status;
	}

	public boolean isFinished()
	{	return status != Status.RUNNING;
	}
}
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

    /**
     * Captures scoring data for undo and redo.
     */
    Snapshot createSnapshot()
    {	return new Snapshot( historyStates, scoredGen, score, status );
    }

    /**
     * Restores scoring data without evaluating the board again.
     */
    void restore( Snapshot snapshot )
    {	historyStates.clear();
        historyStates.addAll( snapshot.historyStates );
        scoredGen = snapshot.scoredGen;
        score = snapshot.score;
        status = snapshot.status;
    }

    /**
     * Stores an independent history set and scoring values.
     * Stored BitSets remain private and are never modified.
     */
    static final class Snapshot
    {
        private final Set<BitSet> historyStates;
        private final int scoredGen;
        private final int score;
        private final Status status;

        private Snapshot( Set<BitSet> historyStates, int scoredGen,
                          int score, Status status )
        {	this.historyStates = new HashSet<BitSet>( historyStates );
            this.scoredGen = scoredGen;
            this.score = score;
            this.status = status;
        }
    }
}
package com.holub.life;

import java.util.Objects;
import java.util.Set;

/** Immutable, validated board data independent of file formats and rendering. */
record BoardSnapshot( int size, Set<BoardSnapshot.Position> liveCells )
{
	record Position( int column, int row )
	{
	}

	BoardSnapshot
	{	if( size <= 0 )
			throw new IllegalArgumentException( "Board size must be positive" );
		liveCells = Set.copyOf( Objects.requireNonNull( liveCells ) );
		for( Position position : liveCells )
		{	if( position.column() < 0 || position.row() < 0
					|| position.column() >= size || position.row() >= size )
				throw new IllegalArgumentException( "Cell outside board: " + position );
		}
	}
}

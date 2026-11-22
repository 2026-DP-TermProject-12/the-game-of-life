package com.holub.life;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** Converts legacy .life coordinate collections without accessing a live board. */
final class LifeFileCodec
{
	/**
	 * The legacy format has no dimensions, so the destination supplies its size.
	 * Leaves the caller-owned input stream open.
	 */
	BoardSnapshot read( InputStream input, int size ) throws IOException
	{	if( size <= 0 )
			throw new IllegalArgumentException( "Board size must be positive" );
		try
		{	Object loaded = new ObjectInputStream( input ).readObject();
			if( !(loaded instanceof Collection<?> coordinates) )
				throw new IOException( "Life file must contain a coordinate collection" );

			Set<BoardSnapshot.Position> positions = new HashSet<>();
			for( Object coordinate : coordinates )
			{	if( !(coordinate instanceof Point point) )
					throw new IOException( "Life file contains an invalid coordinate" );
				if( point.x < 0 || point.y < 0 || point.x >= size || point.y >= size )
					throw new IOException( "Life file contains a cell outside the board" );
				positions.add( new BoardSnapshot.Position( point.x, point.y ) );
			}
			return new BoardSnapshot( size, positions );
		}
		catch( ClassNotFoundException failure )
		{	throw new IOException( "Cannot read Life file class", failure );
		}
	}

	/** Leaves the caller-owned stream open and keeps the original coordinate format. */
	void write( BoardSnapshot snapshot, OutputStream output ) throws IOException
	{	Set<Point> coordinates = new HashSet<>();
		for( BoardSnapshot.Position position : snapshot.liveCells() )
			coordinates.add( new Point( position.column(), position.row() ) );
		ObjectOutputStream encoded = new ObjectOutputStream( output );
		encoded.writeObject( coordinates );
		encoded.flush();
	}
}

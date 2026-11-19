package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.*;
import java.util.*;

import org.junit.jupiter.api.Test;

/***
 * Tests the live-cell memento of {@link Neighborhood}, whose
 * live-cell collection was changed from a LinkedList to a HashSet.
 */

class NeighborhoodStateTest
{
	private static final int GRID_SIZE	= 8;
	private static final int CELL_SIZE	= 8;

	@Test
	void storeThenLoadRestoresLiveCells() throws Exception
	{	Neighborhood board = newBoard();
		click( board, 0, 1 );
		click( board, 3, 4 );
		click( board, 7, 7 );

		byte[] saved = store( board );

		Neighborhood restored = newBoard();
		load( restored, new ByteArrayInputStream( saved ) );

		assertEquals( readLiveCells( saved ), readLiveCells( store( restored ) ) );
		assertTrue( restored.edge( 0, 1 ).isAlive() );
		assertTrue( restored.edge( 7, 7 ).isAlive() );
		assertFalse( restored.edge( 0, 0 ).isAlive() );
	}

	@Test
	void storedFileHoldsEachLiveCellOnce() throws Exception
	{	Neighborhood board = newBoard();
		click( board, 0, 1 );
		click( board, 3, 4 );

		Collection<?> stored = readLiveCells( store( board ) );

		assertEquals( 2, stored.size() );
		assertTrue( stored.contains( new Point( 1, 0 ) ) );
		assertTrue( stored.contains( new Point( 4, 3 ) ) );
	}

	/** Files written before the change hold a serialized LinkedList,
	 *  and must still load.
	 */
	@Test
	void oldLinkedListFileStillLoads() throws Exception
	{	LinkedList<Point> oldFormat = new LinkedList<Point>();
		oldFormat.add( new Point( 1, 0 ) );
		oldFormat.add( new Point( 7, 7 ) );

		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream sink = new ObjectOutputStream( bytes );
		sink.writeObject( oldFormat );
		sink.flush();

		Neighborhood restored = newBoard();
		load( restored, new ByteArrayInputStream( bytes.toByteArray() ) );

		assertTrue( restored.edge( 0, 1 ).isAlive() );
		assertTrue( restored.edge( 7, 7 ).isAlive() );
		assertEquals( new HashSet<Point>( oldFormat ),
					  new HashSet<Object>( readLiveCells( store( restored ) ) ) );
	}

	private Neighborhood newBoard()
	{	return new Neighborhood( GRID_SIZE, new Resident() );
	}

	private void click( Neighborhood board, int row, int column )
	{	Rectangle surface = new Rectangle( 0, 0, GRID_SIZE * CELL_SIZE, GRID_SIZE * CELL_SIZE );
		board.userClicked( new Point( column * CELL_SIZE + 1, row * CELL_SIZE + 1 ), surface );
	}

	/** Same steps as Universe.doStore(). Note that createMemento()
	 *  already stores the live cells, and transfer() stores them
	 *  again, so a list would hold every live cell twice.
	 */
	private byte[] store( Neighborhood board ) throws IOException
	{	Storable memento = board.createMemento();
		board.transfer( memento, new Point( 0, 0 ), Cell.STORE );

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		memento.flush( out );
		return out.toByteArray();
	}

	private void load( Neighborhood board, InputStream in ) throws IOException
	{	board.clear();
		Storable memento = board.createMemento();
		memento.load( in );
		board.transfer( memento, new Point( 0, 0 ), Cell.LOAD );
	}

	private Collection<?> readLiveCells( byte[] saved ) throws Exception
	{	ObjectInputStream source = new ObjectInputStream( new ByteArrayInputStream( saved ) );
		return (Collection<?>)source.readObject();
	}
}

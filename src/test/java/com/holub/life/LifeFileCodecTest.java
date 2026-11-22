package com.holub.life;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class LifeFileCodecTest
{
	private final LifeFileCodec codec = new LifeFileCodec();

	@Test
	void roundTripUsesTheOriginalCoordinateCollection() throws Exception
	{	BoardSnapshot snapshot = new BoardSnapshot( 8,
				Set.of( new BoardSnapshot.Position( 1, 4 ), new BoardSnapshot.Position( 7, 0 ) ) );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		codec.write( snapshot, bytes );
		assertEquals( snapshot, codec.read( new ByteArrayInputStream( bytes.toByteArray() ), 8 ) );
		try( ObjectInputStream input = new ObjectInputStream(
				new ByteArrayInputStream( bytes.toByteArray() ) ) )
		{	assertEquals( Set.of( new Point( 1, 4 ), new Point( 7, 0 ) ), input.readObject() );
		}
	}

	@Test
	void acceptsOriginalLinkedListsAndDeduplicatesCoordinates() throws Exception
	{	byte[] file = encode( new LinkedList<>( List.of( new Point( 3, 2 ), new Point( 3, 2 ) ) ) );
		assertEquals( new BoardSnapshot( 8, Set.of( new BoardSnapshot.Position( 3, 2 ) ) ),
				codec.read( new ByteArrayInputStream( file ), 8 ) );
	}

	@Test
	void acceptsAnEmptyBoard() throws Exception
	{	assertEquals( new BoardSnapshot( 8, Set.of() ),
				codec.read( new ByteArrayInputStream( encode( Set.of() ) ), 8 ) );
	}

	static Stream<Object> invalidFiles()
	{	return Stream.of( "not a collection", List.of( "not a point" ),
				Arrays.asList( new Point( 1, 1 ), null ),
				List.of( new Point( -1, 0 ) ), List.of( new Point( 0, -1 ) ),
				List.of( new Point( 8, 0 ) ), List.of( new Point( 0, 8 ) ) );
	}

	@ParameterizedTest
	@MethodSource( "invalidFiles" )
	void rejectsInvalidTypesAndCoordinatesAsIoErrors( Object data ) throws Exception
	{	byte[] file = encode( data );
		assertThrows( IOException.class, () ->
				codec.read( new ByteArrayInputStream( file ), 8 ) );
	}

	@Test
	void rejectsTruncatedFiles() throws Exception
	{	byte[] file = encode( List.of( new Point( 1, 1 ) ) );
		assertThrows( IOException.class, () -> codec.read(
				new ByteArrayInputStream( Arrays.copyOf( file, file.length / 2 ) ), 8 ) );
	}

	@Test
	void leavesCallerOwnedStreamsOpen() throws Exception
	{	ByteArrayOutputStream output = new ByteArrayOutputStream()
		{
			@Override
			public void close()
			{	fail( "The codec must not close its caller's output" );
			}
		};
		codec.write( new BoardSnapshot( 8, Set.of() ), output );
		ByteArrayInputStream input = new ByteArrayInputStream( output.toByteArray() )
		{
			@Override
			public void close()
			{	fail( "The codec must not close its caller's input" );
			}
		};
		codec.read( input, 8 );
	}

	private byte[] encode( Object value ) throws IOException
	{	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try( ObjectOutputStream output = new ObjectOutputStream( bytes ) )
		{	output.writeObject( value );
		}
		return bytes.toByteArray();
	}
}

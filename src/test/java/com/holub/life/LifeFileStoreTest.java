package com.holub.life;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.awt.Point;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class LifeFileStoreTest
{
	@TempDir
	Path directory;

	@Test
	void successfulSaveReplacesExistingFileUsingLegacyFormat() throws Exception
	{	Path target = existingFile();
		Neighborhood board = new Neighborhood( 4, new Resident() );
		BoardEditor editor = new BoardEditor( board );
		editor.place( new PatternCatalog().copyOf( "Block" ), 1, 1 );
		new LifeFileStore().store( target, editor::store );

		try( ObjectInputStream input = new ObjectInputStream( Files.newInputStream( target ) ) )
		{	assertEquals( Set.of( new Point( 1, 1 ), new Point( 2, 1 ),
					new Point( 1, 2 ), new Point( 2, 2 ) ), input.readObject() );
		}
		assertTrue( editor.canUndo() );
		assertOnlyTargetRemains( target );
	}

	@Test
	void successfulSaveCreatesNewFile() throws Exception
	{	Path target = directory.resolve( "new.life" );
		new LifeFileStore().store( target, output -> output.write( new byte[]{ 1, 2, 3 } ) );
		assertArrayEquals( new byte[]{ 1, 2, 3 }, Files.readAllBytes( target ) );
		assertOnlyTargetRemains( target );
	}

	@Test
	void partialWriteFailurePreservesOriginalFileAndCleansTemporaryFile() throws Exception
	{	Path target = existingFile();
		IOException failure = new IOException( "Write failed" );
		assertSame( failure, assertThrows( IOException.class, () ->
			new LifeFileStore().store( target, output -> {
				output.write( new byte[]{ 9, 8 } );
				assertEquals( "original", Files.readString( target ) );
				throw failure;
			} ) ) );
		assertOriginalPreserved( target );
	}

	@Test
	void closeFailurePreventsReplacementAndPreservesOriginalFile() throws Exception
	{	Path target = existingFile();
		AtomicBoolean replaced = new AtomicBoolean();
		LifeFileStore store = new LifeFileStore( path ->
			new FilterOutputStream( Files.newOutputStream( path ) )
			{
				@Override
				public void close() throws IOException
				{	super.close();
					throw new IOException( "Close failed" );
				}
			}, (source, destination) -> replaced.set( true ) );

		assertThrows( IOException.class, () ->
			store.store( target, output -> output.write( 9 ) ) );
		assertFalse( replaced.get() );
		assertOriginalPreserved( target );
	}

	@Test
	void flushFailurePreventsReplacementAndPreservesOriginalFile() throws Exception
	{	Path target = existingFile();
		AtomicBoolean replaced = new AtomicBoolean();
		LifeFileStore store = new LifeFileStore( path ->
			new FilterOutputStream( Files.newOutputStream( path ) )
			{
				@Override
				public void flush() throws IOException
				{	throw new IOException( "Flush failed" );
				}
			}, (source, destination) -> replaced.set( true ) );

		assertThrows( IOException.class, () ->
			store.store( target, output -> output.write( 9 ) ) );
		assertFalse( replaced.get() );
		assertOriginalPreserved( target );
	}

	@Test
	void replacementFailurePreservesOriginalAndCleansTemporaryFile() throws Exception
	{	Path target = existingFile();
		LifeFileStore store = new LifeFileStore( Files::newOutputStream,
			(source, destination) -> {
				throw new IOException( "Replacement failed" );
			} );

		assertThrows( IOException.class, () ->
			store.store( target, output -> output.write( 9 ) ) );
		assertOriginalPreserved( target );
	}

	@Test
	void unsupportedAtomicMovePreservesOriginalWithoutFallback() throws Exception
	{	Path target = existingFile();
		LifeFileStore store = new LifeFileStore( Files::newOutputStream,
			(source, destination) -> {
				assertEquals( target.getParent(), source.getParent() );
				throw new AtomicMoveNotSupportedException(
						source.toString(), destination.toString(), "Not supported" );
			} );

		assertThrows( AtomicMoveNotSupportedException.class, () ->
			store.store( target, output -> output.write( 9 ) ) );
		assertOriginalPreserved( target );
	}

	@Test
	void outputOpenFailurePreservesOriginalAndCleansTemporaryFile() throws Exception
	{	Path target = existingFile();
		LifeFileStore store = new LifeFileStore( path -> {
			throw new IOException( "Open failed" );
		}, (source, destination) -> fail( "Must not replace after open failure" ) );

		assertThrows( IOException.class, () -> store.store( target, output -> fail() ) );
		assertOriginalPreserved( target );
	}

	@Test
	void failedFirstSaveDoesNotLeaveDestinationOrTemporaryFile() throws Exception
	{	Path target = directory.resolve( "new.life" );
		assertThrows( IOException.class, () -> new LifeFileStore().store( target, output -> {
			output.write( 9 );
			throw new IOException( "Write failed" );
		} ) );
		try( var children = Files.list( directory ) )
		{	assertEquals( 0, children.count() );
		}
	}

	private Path existingFile() throws IOException
	{	return Files.writeString( directory.resolve( "board.life" ), "original" );
	}

	private void assertOriginalPreserved( Path target ) throws IOException
	{	assertEquals( "original", Files.readString( target ) );
		assertOnlyTargetRemains( target );
	}

	private void assertOnlyTargetRemains( Path target ) throws IOException
	{	try( var children = Files.list( directory ) )
		{	assertEquals( List.of( target ), children.toList() );
		}
	}
}

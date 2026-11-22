package com.holub.life;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Owns file streams and atomically replaces files encoded by LifeFileCodec. */
final class LifeFileStore
{
	@FunctionalInterface
	interface Writer
	{
		void write( OutputStream output ) throws IOException;
	}

	@FunctionalInterface
	interface OutputFactory
	{
		OutputStream open( Path path ) throws IOException;
	}

	@FunctionalInterface
	interface Replacement
	{
		void replace( Path source, Path target ) throws IOException;
	}

	private final OutputFactory outputs;
	private final Replacement replacement;
	private final LifeFileCodec codec = new LifeFileCodec();

	LifeFileStore()
	{	this( Files::newOutputStream, (source, target) ->
			Files.move( source, target, StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING ) );
	}

	/** Allows deterministic failures at the output and replacement boundaries. */
	LifeFileStore( OutputFactory outputs, Replacement replacement )
	{	this.outputs = Objects.requireNonNull( outputs );
		this.replacement = Objects.requireNonNull( replacement );
	}

	/** Reads and closes the file before the caller applies the validated result. */
	BoardSnapshot load( Path source, int size ) throws IOException
	{	try( InputStream input = Files.newInputStream( source ) )
		{	return codec.read( input, size );
		}
	}

	void store( Path destination, BoardSnapshot snapshot ) throws IOException
	{	Objects.requireNonNull( snapshot );
		store( destination, output -> codec.write( snapshot, output ) );
	}

	/**
	 * Keeps the destination untouched until writing and closing both succeed.
	 * If atomic replacement is unavailable, saving fails without a non-atomic retry.
	 * The writer must leave the supplied stream open for this store to close.
	 */
	void store( Path destination, Writer writer ) throws IOException
	{	Objects.requireNonNull( writer );
		Path target = Objects.requireNonNull( destination ).toAbsolutePath();
		Path temporary = Files.createTempFile( target.getParent(), ".life-", ".tmp" );
		try
		{	try( OutputStream output = outputs.open( temporary ) )
			{	writer.write( output );
				output.flush();
			}
			replacement.replace( temporary, target );
		}
		catch( IOException | RuntimeException | Error failure )
		{	try
			{	Files.deleteIfExists( temporary );
			}
			catch( IOException cleanupFailure )
			{	failure.addSuppressed( cleanupFailure );
			}
			throw failure;
		}
	}
}

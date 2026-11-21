package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CellState implementations and Resident state transitions.
 */
public class ResidentStateTest
{
	private Resident createAliveResident()
	{	Resident cell = new Resident();
		cell.userClicked( null, null );
		return cell;
	}

	@Test
	void testAliveStateTransitions()
	{	assertEquals( DeadState.INSTANCE,  AliveState.INSTANCE.computeNextState( 0 ) );
		assertEquals( DeadState.INSTANCE,  AliveState.INSTANCE.computeNextState( 1 ) );
		assertEquals( AliveState.INSTANCE, AliveState.INSTANCE.computeNextState( 2 ) );
		assertEquals( AliveState.INSTANCE, AliveState.INSTANCE.computeNextState( 3 ) );
		assertEquals( DeadState.INSTANCE,  AliveState.INSTANCE.computeNextState( 4 ) );
		assertEquals( DeadState.INSTANCE,  AliveState.INSTANCE.computeNextState( 8 ) );
	}

	@Test
	void testDeadStateTransitions()
	{	assertEquals( DeadState.INSTANCE,  DeadState.INSTANCE.computeNextState( 0 ) );
		assertEquals( DeadState.INSTANCE,  DeadState.INSTANCE.computeNextState( 2 ) );
		assertEquals( AliveState.INSTANCE, DeadState.INSTANCE.computeNextState( 3 ) );
		assertEquals( DeadState.INSTANCE,  DeadState.INSTANCE.computeNextState( 4 ) );
	}

	@Test
	void testStateToggle()
	{	assertEquals( DeadState.INSTANCE,  AliveState.INSTANCE.toggle() );
		assertEquals( AliveState.INSTANCE, DeadState.INSTANCE.toggle() );
		assertTrue( AliveState.INSTANCE.isAlive() );
		assertFalse( DeadState.INSTANCE.isAlive() );
	}

	@Test
	void testResidentBirthAndDeath()
	{	Resident cell = new Resident();
		Cell alive = createAliveResident();
		Cell dead  = Cell.DUMMY;

		// Dead cell with 3 live neighbors becomes alive
		assertTrue( cell.figureNextState( alive, alive, alive, dead, dead, dead, dead, dead ) );
		assertTrue( cell.transition() );
		assertTrue( cell.isAlive() );

		// Alive cell with 1 live neighbor dies from underpopulation
		assertTrue( cell.figureNextState( alive, dead, dead, dead, dead, dead, dead, dead ) );
		assertTrue( cell.transition() );
		assertFalse( cell.isAlive() );
	}
}
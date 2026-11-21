package com.holub.life;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/***
 * Tests that the {@link Clock} works without any menu, now that
 * menu creation has moved to {@link ClockMenu}.
 */

class ClockTest
{
	/** Before the change, the constructor added items to the
	 *  MenuSite, so the clock could not be created until the
	 *  menus were established.
	 */
	@Test
	void clockIsCreatedWithoutMenuSite()
	{	assertDoesNotThrow( () -> Clock.instance() );
	}

	@Test
	void tickNotifiesListener()
	{	AtomicInteger ticks = new AtomicInteger();
		Clock.instance().addClockListener( () -> ticks.incrementAndGet() );

		Clock.instance().tick();
		Clock.instance().tick();

		assertEquals( 2, ticks.get() );
	}
}

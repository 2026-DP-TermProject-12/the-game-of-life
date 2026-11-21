package com.holub.life;

import java.awt.event.ActionListener;

import com.holub.ui.MenuSite;

/***
 * The <code>ClockMenu</code> puts the "Go" menu, which controls
 * the speed of a {@link Clock}, onto the {@link MenuSite}.
 * <p>
 * The menu used to be created by the <code>Clock</code> constructor,
 * so the clock was responsible for both timing and user interface.
 * Moving the menu here leaves the <code>Clock</code> with timing
 * only, so it can be created and driven without any menu (in unit
 * tests, for example), and the menu can change without touching
 * the clock.
 *
 * @see Clock
 */

public class ClockMenu
{
	/** Milliseconds between ticks for the "Agonizing" speed. */
	public static final int AGONIZING_DELAY = 500;

	/** Milliseconds between ticks for the "Slow" speed. */
	public static final int SLOW_DELAY		= 150;

	/** Milliseconds between ticks for the "Medium" speed. */
	public static final int MEDIUM_DELAY	= 70;

	/** Milliseconds between ticks for the "Fast" speed. */
	public static final int FAST_DELAY		= 30;

	private final Clock clock;

	/** Create a menu that controls the indicated clock. The menu
	 *  is not displayed until {@link #install} is called.
	 *  @param clock The clock whose speed is set by this menu.
	 */
	public ClockMenu( Clock clock )
	{	this.clock = clock;
	}

	/** Put the speed-control items onto the "Go" menu. The
	 *  {@link MenuSite} must be established before this method
	 *  is called.
	 */
	public void install()
	{	MenuSite.addLine( this, "Go", "Halt",				e -> clock.stop() );
		MenuSite.addLine( this, "Go", "Tick (Single Step)",	e -> clock.tick() );
		MenuSite.addLine( this, "Go", "Agonizing",			speed( AGONIZING_DELAY ) );
		MenuSite.addLine( this, "Go", "Slow",				speed( SLOW_DELAY ) );
		MenuSite.addLine( this, "Go", "Medium",				speed( MEDIUM_DELAY ) );
		MenuSite.addLine( this, "Go", "Fast",				speed( FAST_DELAY ) );
	}

	/** Remove the items added by {@link #install} from the menu site. */
	public void uninstall()
	{	MenuSite.removeMyMenus( this );
	}

	/** Return a listener that starts the clock with the indicated
	 *  delay. Each menu item gets its own listener, so the item no
	 *  longer has to be identified by the first letter of its name.
	 */
	private ActionListener speed( int millisecondsBetweenTicks )
	{	return e -> clock.startTicking( millisecondsBetweenTicks );
	}
}

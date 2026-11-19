package com.holub.life;

/** Selects the neighboring block beyond the outermost board. */
enum BoundaryPolicy
{
	FIXED
	{
		@Override
		Cell outsideNeighbor( Cell board )
		{	return Cell.DUMMY;
		}
	},
	WRAP_AROUND
	{
		@Override
		Cell outsideNeighbor( Cell board )
		{	return board;
		}
	};

	abstract Cell outsideNeighbor( Cell board );
}

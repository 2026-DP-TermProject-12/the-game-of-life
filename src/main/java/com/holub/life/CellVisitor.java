package com.holub.life;

/**
 * Visitor interface for traversing the Cell composite structure.
 */
public interface CellVisitor
{
	void visit( Resident cell );
	void visit( Neighborhood grid );
}
package com.holub.life;

import com.holub.life.LifePattern.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LifePatternTest
{
	private static final List<Position> ORIGINAL_CELLS = List.of(
			new Position( 0, 0 ),
			new Position( 2, 0 ),
			new Position( 1, 1 ) );

	@Test
	@DisplayName( "생성자에 넘긴 목록을 수정해도 패턴의 크기와 생존 셀을 유지한다" )
	void constructorCopiesInputPositions()
	{	List<Position> input = new ArrayList<>( ORIGINAL_CELLS );
		LifePattern pattern = new LifePattern( 4, 3, input );

		input.clear();
		input.add( new Position( 3, 2 ) );

		assertPattern( pattern, 4, 3, Set.copyOf( ORIGINAL_CELLS ) );
	}

	@Test
	@DisplayName( "조회한 생존 셀 목록에는 셀을 추가할 수 없다" )
	void liveCellsRejectsExternalModification()
	{	LifePattern pattern = asymmetricPattern();

		assertThrows( UnsupportedOperationException.class,
				() -> pattern.liveCells().add( new Position( 3, 2 ) ) );
		assertPattern( pattern, 4, 3, Set.copyOf( ORIGINAL_CELLS ) );
	}

	@Test
	@DisplayName( "회전 전에 조회한 생존 셀 목록에도 회전한 좌표가 반영된다" )
	void liveCellsViewReflectsLaterTransformations()
	{	LifePattern pattern = asymmetricPattern();
		List<Position> view = pattern.liveCells();

		pattern.rotateClockwise();

		assertEquals( 3, view.size(), "생존 셀 수" );
		assertEquals( Set.of( new Position( 2, 0 ), new Position( 2, 2 ), new Position( 1, 1 ) ),
				Set.copyOf( view ), "회전 전에 조회한 목록의 생존 좌표" );
	}

	@Test
	@DisplayName( "복사본은 회전된 상태를 이어받고 원본과 독립적으로 변형된다" )
	void copyPreservesCurrentOrientationAndIsIndependent()
	{	LifePattern original = asymmetricPattern();
		original.rotateClockwise();
		LifePattern copy = original.copy();
		Set<Position> rotatedCells = Set.of(
				new Position( 2, 0 ), new Position( 2, 2 ), new Position( 1, 1 ) );
		Set<Position> mirroredCells = Set.of(
				new Position( 0, 0 ), new Position( 0, 2 ), new Position( 1, 1 ) );
		assertPattern( copy, 3, 4, rotatedCells );

		copy.flipHorizontally();
		assertPattern( copy, 3, 4, mirroredCells );
		assertPattern( original, 3, 4, rotatedCells );

		original.rotateClockwise();
		assertPattern( original, 4, 3, Set.of(
				new Position( 3, 2 ), new Position( 1, 2 ), new Position( 2, 1 ) ) );
		assertPattern( copy, 3, 4, mirroredCells );
	}

	@Test
	@DisplayName( "시계 방향 회전은 생존 좌표를 옮기고 빈 여백을 포함해 가로와 세로를 맞바꾼다" )
	void rotateClockwiseRemapsCellsAndSwapsDimensions()
	{	LifePattern pattern = asymmetricPattern();

		pattern.rotateClockwise();

		assertPattern( pattern, 3, 4, Set.of(
				new Position( 2, 0 ), new Position( 2, 2 ), new Position( 1, 1 ) ) );
	}

	@Test
	@DisplayName( "좌우 반전은 크기와 행을 유지하면서 열만 대칭 이동한다" )
	void flipHorizontallyMirrorsColumnsOnly()
	{	LifePattern pattern = asymmetricPattern();

		pattern.flipHorizontally();

		assertPattern( pattern, 4, 3, Set.of(
				new Position( 3, 0 ), new Position( 1, 0 ), new Position( 2, 1 ) ) );
	}

	@Test
	@DisplayName( "네 번 회전하면 처음 크기와 생존 좌표를 복원한다" )
	void fourRotationsRestoreOriginalPattern()
	{	LifePattern pattern = asymmetricPattern();

		for( int rotation = 0; rotation < 4; rotation++ )
			pattern.rotateClockwise();

		assertPattern( pattern, 4, 3, Set.copyOf( ORIGINAL_CELLS ) );
	}

	@Test
	@DisplayName( "두 번 좌우 반전하면 처음 크기와 생존 좌표를 복원한다" )
	void twoHorizontalFlipsRestoreOriginalPattern()
	{	LifePattern pattern = asymmetricPattern();

		pattern.flipHorizontally();
		pattern.flipHorizontally();

		assertPattern( pattern, 4, 3, Set.copyOf( ORIGINAL_CELLS ) );
	}

	@Test
	@DisplayName( "회전 직후 좌우 반전하면 회전된 폭을 기준으로 좌표를 옮긴다" )
	void rotateThenFlipUsesRotatedWidth()
	{	LifePattern pattern = asymmetricPattern();

		pattern.rotateClockwise();
		pattern.flipHorizontally();

		assertPattern( pattern, 3, 4, Set.of(
				new Position( 0, 0 ), new Position( 0, 2 ), new Position( 1, 1 ) ) );
	}

	@ParameterizedTest( name = "{0}" )
	@MethodSource( "degeneratePatterns" )
	@DisplayName( "한 칸, 한 줄, 빈 패턴도 회전과 좌우 반전 후 크기와 좌표가 정확하다" )
	void transformDegeneratePatterns( String caseName, int width, int height, List<Position> cells,
			Set<Position> flippedCells, Set<Position> rotatedCells )
	{	LifePattern flipped = new LifePattern( width, height, cells );
		flipped.flipHorizontally();
		assertPattern( flipped, width, height, flippedCells );

		LifePattern rotated = new LifePattern( width, height, cells );
		rotated.rotateClockwise();
		assertPattern( rotated, height, width, rotatedCells );
	}

	private static Stream<Arguments> degeneratePatterns()
	{	return Stream.of(
			Arguments.of( "1×1 생존 셀은 두 변환 후에도 제자리에 있다", 1, 1,
					List.of( new Position( 0, 0 ) ),
					Set.of( new Position( 0, 0 ) ), Set.of( new Position( 0, 0 ) ) ),
			Arguments.of( "폭이 1인 패턴은 회전하면 가로 한 줄이 된다", 1, 4,
					List.of( new Position( 0, 0 ), new Position( 0, 2 ) ),
					Set.of( new Position( 0, 0 ), new Position( 0, 2 ) ),
					Set.of( new Position( 3, 0 ), new Position( 1, 0 ) ) ),
			Arguments.of( "높이가 1인 패턴은 회전하면 세로 한 줄이 된다", 4, 1,
					List.of( new Position( 0, 0 ), new Position( 2, 0 ) ),
					Set.of( new Position( 3, 0 ), new Position( 1, 0 ) ),
					Set.of( new Position( 0, 0 ), new Position( 0, 2 ) ) ),
			Arguments.of( "빈 4×3 패턴은 회전하면 빈 3×4 패턴이 된다", 4, 3,
					List.of(), Set.of(), Set.of() ) );
	}

	private static LifePattern asymmetricPattern()
	{	return new LifePattern( 4, 3, ORIGINAL_CELLS );
	}

	private static void assertPattern( LifePattern pattern, int width, int height, Set<Position> cells )
	{	assertEquals( width, pattern.width(), "패턴의 폭" );
		assertEquals( height, pattern.height(), "패턴의 높이" );
		assertEquals( cells.size(), pattern.liveCells().size(), "생존 셀 수" );
		assertEquals( cells, Set.copyOf( pattern.liveCells() ), "생존 셀 좌표" );
	}
}

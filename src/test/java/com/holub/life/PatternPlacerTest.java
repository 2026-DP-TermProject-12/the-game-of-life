package com.holub.life;

import com.holub.life.LifePattern.Position;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PatternPlacerTest
{
	private static final int BOARD_SIZE = 8;
	private final PatternPlacer placer = new PatternPlacer();

	@Nested
	@DisplayName( "패턴 배치와 기존 생존 셀" )
	class Placement
	{
		private final Cell board = new Neighborhood( BOARD_SIZE, new Resident() );

		@Test
		@DisplayName( "열과 행이 다른 원점을 기준으로 패턴의 생존 셀만 배치한다" )
		void placesLiveCellsAtOffsetOrigin()
		{	assertTrue( placer.place( board, asymmetricPattern(), 2, 3 ) );
			assertEquals( Set.of( new Position( 2, 3 ), new Position( 4, 3 ), new Position( 3, 4 ) ),
					liveCells( board ) );
		}

		@Test
		@DisplayName( "패턴의 생존 칸과 빈 칸에 겹치는 셀 및 패턴 바깥에 있는 생존 셀을 보존한다" )
		void preservesExistingLiveCellsInsideAndOutsidePattern()
		{	clickCell( board, 2, 3 );
			clickCell( board, 3, 3 );
			clickCell( board, 0, 7 );

			assertTrue( placer.place( board, asymmetricPattern(), 2, 3 ) );
			assertEquals( Set.of(
					new Position( 2, 3 ), new Position( 3, 3 ), new Position( 0, 7 ),
					new Position( 4, 3 ), new Position( 3, 4 ) ), liveCells( board ) );
		}

		@Test
		@DisplayName( "같은 위치에 두 번 배치해도 두 호출이 모두 성공하고 첫 배치 결과를 유지한다" )
		void placingSamePatternTwiceLeavesBoardUnchanged()
		{	LifePattern pattern = asymmetricPattern();
			Set<Position> expected = Set.of(
					new Position( 2, 3 ), new Position( 4, 3 ), new Position( 3, 4 ) );

			assertTrue( placer.place( board, pattern, 2, 3 ), "첫 번째 배치" );
			assertEquals( expected, liveCells( board ), "첫 번째 배치 후 생존 셀" );

			assertTrue( placer.place( board, pattern, 2, 3 ), "두 번째 배치" );
			assertEquals( expected, liveCells( board ), "두 번째 배치 후 생존 셀" );
		}

		@Test
		@DisplayName( "보드 안에 들어오는 빈 패턴은 배치에 성공하고 기존 상태를 유지한다" )
		void acceptsEmptyPatternWithinBoundsWithoutChangingBoard()
		{	clickCell( board, 4, 5 );
			clickCell( board, 0, 7 );
			Set<Position> before = liveCells( board );

			assertTrue( placer.place( board, new LifePattern( 4, 3, List.of() ), 4, 5 ) );
			assertEquals( before, liveCells( board ) );
		}
	}

	@Nested
	@DisplayName( "패턴 크기와 보드 경계" )
	class BoundaryChecks
	{
		private final Cell board = new Neighborhood( BOARD_SIZE, new Resident() );

		@ParameterizedTest( name = "{0} 모서리 원점 ({1}, {2})에 배치한다" )
		@MethodSource( "cornerPlacements" )
		@DisplayName( "가장자리까지 닿는 패턴을 네 모서리에 정확히 배치한다" )
		void acceptsPatternExactlyAtEachCorner( String corner, int column, int row, Set<Position> expected )
		{	LifePattern pattern = new LifePattern( 4, 3,
					List.of( new Position( 0, 0 ), new Position( 3, 2 ) ) );

			assertTrue( placer.place( board, pattern, column, row ) );
			assertEquals( expected, liveCells( board ) );
		}

		private static Stream<Arguments> cornerPlacements()
		{	return Stream.of(
					Arguments.of( "왼쪽 위", 0, 0, Set.of( new Position( 0, 0 ), new Position( 3, 2 ) ) ),
					Arguments.of( "오른쪽 위", 4, 0, Set.of( new Position( 4, 0 ), new Position( 7, 2 ) ) ),
					Arguments.of( "왼쪽 아래", 0, 5, Set.of( new Position( 0, 5 ), new Position( 3, 7 ) ) ),
					Arguments.of( "오른쪽 아래", 4, 5, Set.of( new Position( 4, 5 ), new Position( 7, 7 ) ) ) );
		}

		@Test
		@DisplayName( "보드와 크기가 같은 패턴을 원점에 배치하면 마지막 열과 행까지 도달한다" )
		void acceptsPatternMatchingEntireBoard()
		{	LifePattern pattern = new LifePattern( 8, 8, List.of(
					new Position( 0, 0 ), new Position( 7, 1 ),
					new Position( 2, 7 ), new Position( 7, 7 ) ) );

			assertTrue( placer.place( board, pattern, 0, 0 ) );
			assertEquals( Set.of( new Position( 0, 0 ), new Position( 7, 1 ),
					new Position( 2, 7 ), new Position( 7, 7 ) ), liveCells( board ) );
		}

		@ParameterizedTest( name = "{0}이면 보드를 수정하지 않고 배치를 거부한다" )
		@CsvSource( {
				"음수 열 원점, -1,  0, 4, 3",
				"음수 행 원점,  0, -1, 4, 3",
				"오른쪽 경계를 한 칸 초과, 5, 0, 4, 3",
				"아래쪽 경계를 한 칸 초과, 0, 6, 4, 3",
				"보드보다 넓은 패턴, 0, 0, 9, 3",
				"보드보다 높은 패턴, 0, 0, 4, 9"
		} )
		@DisplayName( "원점 또는 패턴 크기가 경계를 벗어나면 일부 셀도 배치하지 않는다" )
		void rejectsOutOfBoundsPlacementWithoutChangingBoard( String caseName, int column, int row,
				int width, int height )
		{	LifePattern pattern = new LifePattern( width, height,
					List.of( new Position( 0, 0 ), new Position( width - 1, height - 1 ) ) );
			clickCell( board, 1, 4 );
			Set<Position> before = liveCells( board );

			assertFalse( placer.place( board, pattern, column, row ) );
			assertEquals( before, liveCells( board ) );
		}

		@ParameterizedTest( name = "{0} 빈 여백만 넘어도 배치를 거부한다" )
		@CsvSource( { "오른쪽, 5, 0", "아래쪽, 0, 6" } )
		@DisplayName( "생존 셀이 보드 안에 있어도 빈 여백이 경계를 넘으면 보드를 그대로 둔다" )
		void rejectsPlacementWhenOnlyEmptyMarginOverflows( String edge, int column, int row )
		{	LifePattern pattern = new LifePattern( 4, 3, List.of( new Position( 0, 0 ) ) );
			clickCell( board, 1, 4 );
			Set<Position> before = liveCells( board );

			assertFalse( placer.place( board, pattern, column, row ) );
			assertEquals( before, liveCells( board ) );
		}

		@Test
		@DisplayName( "회전한 Blinker는 현재 크기로 경계를 검사하고 현재 좌표로 배치된다" )
		void usesCurrentOrientationForBoundsAndPlacement()
		{	LifePattern pattern = new PatternCatalog().copyOf( "Blinker" );

			assertFalse( placer.place( board, pattern, 6, 1 ), "가로 상태에서는 경계 초과" );
			assertEquals( Set.of(), liveCells( board ), "실패한 배치 후 생존 셀" );

			pattern.rotateClockwise();

			assertTrue( placer.place( board, pattern, 6, 1 ), "세로 상태에서는 같은 원점에 배치 가능" );
			assertEquals( Set.of( new Position( 6, 1 ), new Position( 6, 2 ), new Position( 6, 3 ) ),
					liveCells( board ) );
		}
	}

	@Nested
	@DisplayName( "기존 보드 동작과의 연동" )
	class BoardBehavior
	{
		@Test
		@DisplayName( "프리셋 배치 후 일반 클릭으로 선택한 셀만 죽였다가 다시 살릴 수 있다" )
		void ordinaryBoardClickStillTogglesCellAfterPlacement()
		{	Cell board = new Neighborhood( BOARD_SIZE, new Resident() );
			LifePattern blinker = new PatternCatalog().copyOf( "Blinker" );
			assertTrue( placer.place( board, blinker, 2, 3 ) );
			Set<Position> expected = Set.of(
					new Position( 2, 3 ), new Position( 3, 3 ), new Position( 4, 3 ) );
			assertEquals( expected, liveCells( board ), "배치 직후 생존 셀" );

			clickCell( board, 3, 3 );
			assertEquals( Set.of( new Position( 2, 3 ), new Position( 4, 3 ) ),
					liveCells( board ), "한 번 클릭한 후 생존 셀" );

			clickCell( board, 3, 3 );
			assertEquals( expected, liveCells( board ), "두 번 클릭한 후 생존 셀" );
		}

		@Test
		@DisplayName( "내부 구역 경계에 걸쳐 배치한 Blinker는 한 세대 후 세로가 되고 두 세대 후 가로로 돌아온다" )
		void placedBlinkerOscillatesAcrossNeighborhoodBoundary()
		{	Cell board = new Neighborhood( 2, new Neighborhood( 4, new Resident() ) );
			LifePattern blinker = new PatternCatalog().copyOf( "Blinker" );
			Set<Position> horizontal = Set.of(
					new Position( 2, 3 ), new Position( 3, 3 ), new Position( 4, 3 ) );

			assertTrue( placer.place( board, blinker, 2, 3 ) );
			assertEquals( horizontal, liveCells( board ), "초기 가로 상태" );

			advanceGeneration( board );
			assertEquals( Set.of( new Position( 3, 2 ), new Position( 3, 3 ), new Position( 3, 4 ) ),
					liveCells( board ), "한 세대가 지난 세로 상태" );

			advanceGeneration( board );
			assertEquals( horizontal, liveCells( board ), "두 세대가 지난 가로 상태" );
		}

		@Test
		@DisplayName( "보드 내부에 배치한 Block은 한 세대가 지나도 네 생존 셀을 유지한다" )
		void placedBlockRemainsStableAfterGeneration()
		{	Cell board = new Neighborhood( BOARD_SIZE, new Resident() );
			LifePattern block = new PatternCatalog().copyOf( "Block" );
			Set<Position> expected = Set.of( new Position( 2, 3 ), new Position( 3, 3 ),
					new Position( 2, 4 ), new Position( 3, 4 ) );

			assertTrue( placer.place( board, block, 2, 3 ) );
			assertEquals( expected, liveCells( board ), "배치 직후 생존 셀" );

			advanceGeneration( board );
			assertEquals( expected, liveCells( board ), "한 세대가 지난 뒤 생존 셀" );
		}
	}

	private static LifePattern asymmetricPattern()
	{	return new LifePattern( 4, 3, List.of(
				new Position( 0, 0 ), new Position( 2, 0 ), new Position( 1, 1 ) ) );
	}

	private static void clickCell( Cell board, int column, int row )
	{	int size = board.widthInCells();
		board.userClicked( new Point( column, row ), new Rectangle( 0, 0, size, size ) );
	}

	private static Set<Position> liveCells( Cell board )
	{	Cell.Memento snapshot = (Cell.Memento) board.createMemento();
		int size = board.widthInCells();
		Set<Position> positions = new HashSet<>();
		for( int row = 0; row < size; row++ )
		{	for( int column = 0; column < size; column++ )
			{	if( snapshot.isAlive( new Point( column, row ) ) )
					positions.add( new Position( column, row ) );
			}
		}
		return positions;
	}

	private static void advanceGeneration( Cell board )
	{	board.figureNextState( Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY,
				Cell.DUMMY, Cell.DUMMY, Cell.DUMMY, Cell.DUMMY );
		board.transition();
	}
}

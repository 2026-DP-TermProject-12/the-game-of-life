package com.holub.life;

import com.holub.life.LifePattern.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PatternCatalogTest
{
	private static final Set<Position> GLIDER_CELLS = Set.of(
			new Position( 1, 0 ), new Position( 2, 1 ),
			new Position( 0, 2 ), new Position( 1, 2 ), new Position( 2, 2 ) );

	private final PatternCatalog catalog = new PatternCatalog();

	@Test
	@DisplayName( "기본 프리셋 이름을 메뉴의 등록 순서대로 제공한다" )
	void listsPresetNamesInMenuOrder()
	{	assertEquals( List.of( "Glider", "Blinker", "Block" ),
			List.copyOf( catalog.names() ) );
	}

	@Test
	@DisplayName( "이름 목록에서 Glider를 제거할 수 없고 이후에도 선택할 수 있다" )
	void presetNamesCannotBeRemoved()
	{	Set<String> names = catalog.names();

		assertThrows( UnsupportedOperationException.class, () -> names.remove( "Glider" ) );
		assertPattern( catalog.copyOf( "Glider" ), 3, 3, GLIDER_CELLS );
	}

	@ParameterizedTest( name = "{0}의 초기 크기와 생존 좌표" )
	@MethodSource( "builtInPatterns" )
	@DisplayName( "세 기본 프리셋의 크기와 생존 좌표가 정의와 일치한다" )
	void returnsExpectedBuiltInPattern( String name, int width, int height, Set<Position> cells )
	{	assertPattern( catalog.copyOf( name ), width, height, cells );
	}

	private static Stream<Arguments> builtInPatterns()
	{	return Stream.of(
			Arguments.of( "Glider", 3, 3, GLIDER_CELLS ),
			Arguments.of( "Blinker", 3, 1, Set.of(
					new Position( 0, 0 ), new Position( 1, 0 ), new Position( 2, 0 ) ) ),
			Arguments.of( "Block", 2, 2, Set.of(
					new Position( 0, 0 ), new Position( 1, 0 ),
					new Position( 0, 1 ), new Position( 1, 1 ) ) ) );
	}

	@Test
	@DisplayName( "Glider 선택 하나를 변형해도 다른 선택과 이후 재선택은 초기 모양을 유지한다" )
	void transformingOneSelectionKeepsOtherSelectionsAndPrototypeUnchanged()
	{	LifePattern first = catalog.copyOf( "Glider" );
		LifePattern second = catalog.copyOf( "Glider" );

		first.rotateClockwise();
		first.flipHorizontally();

		assertPattern( first, 3, 3, Set.of(
				new Position( 0, 1 ), new Position( 1, 2 ),
				new Position( 2, 0 ), new Position( 2, 1 ), new Position( 2, 2 ) ) );
		assertPattern( second, 3, 3, GLIDER_CELLS );
		assertPattern( catalog.copyOf( "Glider" ), 3, 3, GLIDER_CELLS );
	}

	@ParameterizedTest( name = "등록되지 않은 이름 \"{0}\"을 거부한다" )
	@ValueSource( strings = { "Unknown", "glider", "Glider ", "" } )
	@DisplayName( "등록된 이름과 정확히 일치할 때만 프리셋을 선택할 수 있다" )
	void rejectsUnknownOrNonExactPresetNames( String name )
	{	assertThrows( IllegalArgumentException.class, () -> catalog.copyOf( name ) );
	}

	private static void assertPattern( LifePattern pattern, int width, int height, Set<Position> cells )
	{	assertEquals( width, pattern.width(), "프리셋의 폭" );
		assertEquals( height, pattern.height(), "프리셋의 높이" );
		assertEquals( cells.size(), pattern.liveCells().size(), "생존 셀 수" );
		assertEquals( cells, Set.copyOf( pattern.liveCells() ), "생존 셀 좌표" );
	}
}

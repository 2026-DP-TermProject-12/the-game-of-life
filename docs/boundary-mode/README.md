# 경계 모드와 세대 진행 분리

김태우 · 설계패턴 텀프로젝트 12팀

## 기능 확장

`Grid`에서 `Boundary (Fixed)`를 열어 경계 모드를 고른다. 앱을 처음 실행하면 고정 경계가 선택된다. 선택 직후 자동 진행이 멈추며, 메뉴 이름에 현재 모드가 표시된다. 셀 배치는 유지되고 `Go > Tick (Single Step)`이나 자동 진행의 다음 세대부터 선택한 경계를 적용한다.

| 선택 | 바깥 이웃 계산 |
| --- | --- |
| **Fixed** | 판 바깥의 8방향 이웃을 `Cell.DUMMY`로 처리한다. |
| **Wrap Around** | 상하·좌우의 맞은편을 이웃으로 연결한다. 모서리 밖 대각선 방향은 반대쪽 모서리로 이어진다. |

`Grid > Clear`는 셀을 지운다. `Grid > Store` 및 `Grid > Load`는 기존 `.life` 파일을 사용한다. 파일에는 살아 있는 셀 좌표가 들어가며, 불러온 판에는 화면에서 선택한 경계 모드가 적용된다.

## 설계 변경

적용 기준은 첨부한 코드와 팀 `main`의 `0ee9cf7`이다. 최초 기능 코드 `74b4563`과 비교하면 기존 `Universe`의 클록 리스너는 8방향 모두에 `Cell.DUMMY`를 넘긴 다음 `transition()`을 호출했다. `LifeBoard.step()`이 정책 선택, 다음 상태 계산, 세대 반영을 맡게 분리했다. `Universe`는 메뉴 이벤트를 전달하고 실제 상태가 바뀌면 화면을 갱신한다.

- **Strategy**. `LifeBoard`가 선택한 `BoundaryPolicy`에 바깥 이웃을 요청한다. `FIXED`는 `Cell.DUMMY`를 반환한다. `WRAP_AROUND`는 최상위 `Neighborhood`를 반환한다. `Neighborhood.edge()`가 기존 Composite 구조에서 맞은편의 하위 블록과 셀을 찾는다.
- **계산 후 반영**. 모든 대상 셀의 다음 상태를 계산한 뒤 한 번에 반영한다. 연결 경계도 같은 세대의 상태에서 이웃을 읽는다. 모드를 변경하거나 파일을 복원하면 다음 `step()`에서 전체 블록을 한 번 방문한다. 이후에는 기존 `amActive`와 `activeEdges`로 방문할 블록을 판단하고, 안정 상태에 도달하면 최상위 블록의 지난 가장자리 플래그를 지운다.
- **블록 경계 전파**. 기존 `Resident.transition()`이 안정된 셀에서 `true`를 반환하던 부분을 실제 변경 시 `true`로 고쳤다. `Neighborhood`가 다음 세대에 다시 계산할 인접 블록을 올바르게 표시한다.
- **이벤트 순서**. 자동 진행을 Swing 타이머에서 처리한다. 메뉴 선택, 셀 클릭, 한 세대의 계산과 반영이 같은 이벤트 스레드에서 순서대로 실행된다.

## 검토한 대안

| 결정 | 대안과 선택 이유 |
| --- | --- |
| 바깥 이웃 선택 | `Universe`의 조건문이면 두 모드를 적은 코드로 구현할 수 있다. 경계 선택을 `BoundaryPolicy`에 두면 화면 처리와 세대 진행이 정책별 로직을 알 필요가 없다. |
| 이웃을 찾는 방식 | 전체 좌표를 평탄화해 행·열마다 나머지 연산을 할 수도 있다. 최상위 보드를 넘기면 기존 `edge()`와 블록 갱신 방식을 재사용할 수 있다. |
| 안정 블록 재평가 | 매 세대 전체 판을 훑으면 단순하지만 기존 안정 블록 생략을 잃는다. 모드 변경과 복원 직후 한 세대만 전체를 다시 계산한다. |

## UML

- [클래스 다이어그램](boundary-classes.svg) · [PlantUML 원본](boundary-classes.puml)
- [시퀀스 다이어그램](boundary-sequence.svg) · [PlantUML 원본](boundary-sequence.puml)
- [유스케이스 다이어그램](boundary-use-case.svg) · [PlantUML 원본](boundary-use-case.puml)

## JUnit 테스트

`LifeBoardTest`는 Swing 창 없이 실제 64×64 Composite 보드를 만들고 매 검증 시점에 4,096개 좌표를 확인한다. 좌표는 0부터 세며 `(행, 열)` 순서로 적었다.

| 테스트 | 목적과 확인한 결과 |
| --- | --- |
| `fixedBoundaryIsDefaultAndDoesNotConnectOppositeEdges` | 기본 모드에서 `(30,63)`, `(30,0)`, `(30,1)`이 한 세대 후 모두 소멸한다. |
| `wrapConnectsLeftAndRightEdgesAcrossGenerations` | 같은 배치가 0열의 세로 3칸이 되고, 한 세대 더 지나면 시작 배치로 돌아온다. |
| `wrapConnectsTopAndBottomEdges` | 30열의 63·0·1행에 놓인 셀이 한 세대 뒤 0행의 29·30·31열로 바뀐다. |
| `wrapCountsTheDiagonallyOppositeCorner` | 세 모서리가 살면 빈 모서리가 태어난다. 네 모서리가 안정되면 최상위 가장자리 플래그가 해제된다. |
| `switchingToWrapRechecksStableBlocksAndPreservesTheCurrentGeneration` | 고정 경계에서 안정화된 양 끝 2×2 블록은 모드 선택 순간 유지된다. 다음 세대에는 맞닿은 열의 과밀로 기존 셀 네 개가 사라지고 네 개가 태어난다. |
| `switchingBackToFixedRechecksAStableWrappedBlock` | 연결 상태에서 양 끝에 걸친 안정 블록을 고정 경계로 바꾸면 다음 세대에 모두 소멸한다. |
| `legacySaveRecalculatesBirthAcrossAnInactiveBlockBoundary` | 기존 포맷으로 복원한 7열의 세로 3칸이 다음 세대에 6·7·8열의 가로 3칸으로 바뀐다. |
| `saveUsesLegacyCoordinatesAndLoadUsesTheCurrentlySelectedPolicy` | 연결 경계에서 저장한 파일은 기존 좌표 컬렉션으로 읽히며, 고정 경계에서 불러오면 그 정책으로 진행된다. |
| `failedReadLeavesTheExistingPlacementAvailable` | 읽기 오류가 발생해도 현재 배치와 이후 세대 진행이 보존된다. |
| `aRealChangeActivatesTheNeighboringBlockOnTheNextStep` | 일반 글라이더가 열 7·8 사이 블록 경계를 넘어가며 세대 8, 12의 예상 위치에 도달한다. |

Windows PowerShell에서는 `.\gradlew.bat clean test jar`, macOS/Linux에서는 `./gradlew clean test jar`를 실행한다. 실행 결과는 `build/reports/tests/test/index.html`에서 확인할 수 있다. 게임은 `java -jar build/libs/Life-1.0.jar`로 실행한다. JDK 21을 사용했다.

[JUnit 실행 결과](junit-results.png)에서 10개 통과와 0개 실패를 확인할 수 있다.

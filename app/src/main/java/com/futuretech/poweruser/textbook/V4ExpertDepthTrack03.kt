package com.futuretech.poweruser.textbook

internal object V4ExpertDepthTrack03 {
    val packs: List<ExpertDepthPack> = listOf(
        expertPack(
            sectionId = "V1-C03-S01",
            depthTitle("배열·연결·해시를 실제 성능으로 이어 주는 세 개념: locality·amortized cost·collision"),
            depthParagraph("""
V3에서 array, dynamic array, linked list, node를 실제 코드로 배웠다면 이번에는 왜 현대 시스템에서 이론적 Big-O만으로 선택하면 부족한지 본다. 메모리 locality, CPU cache, allocation overhead, resizing 방식이 실제 성능을 크게 바꾼다. “중간 삽입 O(1)인 linked list가 array보다 항상 빠르다” 같은 결론이 틀릴 수 있는 이유다.
"""),
            depthHeading("1. dynamic array의 resize는 매번 일어나지 않는다"),
            depthParagraph("""
capacity가 가득 찰 때마다 딱 1칸만 늘리면 매 append마다 기존 원소를 복사해야 해 전체 비용이 커진다. 많은 구현은 capacity를 일정 비율로 크게 늘려 드문 시점에 여러 원소를 복사한다. 한 번의 resize는 O(n)이지만 긴 append 연산 전체를 평균 내면 한 번당 amortized O(1)로 볼 수 있다. “최악 한 번”과 “긴 작업열의 평균 비용”을 구분하는 첫 사례다.
"""),
            depthHeading("2. linked list는 pointer를 따라가느라 cache locality가 나쁠 수 있다"),
            depthParagraph("""
배열 원소는 메모리에 연속 배치되는 경우가 많아 CPU가 다음 데이터를 미리 가져오기 쉽다. linked list node는 heap 여기저기에 흩어질 수 있어 매 node마다 다른 cache line을 기다릴 수 있다. 따라서 순차 순회는 이론상 둘 다 O(n)이어도 배열이 훨씬 빠른 경우가 많다. 자료구조 선택에서 하드웨어 비용까지 보는 이유다.
"""),
            depthHeading("3. linked list 중간 삽입 O(1)에는 숨은 전제가 있다"),
            depthParagraph("""
삽입할 node의 정확한 위치를 이미 가리키는 reference가 있을 때 연결 변경은 O(1)이다. “100000번째 위치에 삽입”하려고 처음부터 node를 따라가야 하면 위치 탐색이 O(n)이다. Big-O 문장을 읽을 때 무엇이 입력으로 이미 주어졌는지 확인하지 않으면 잘못된 비교가 된다.
"""),
            depthHeading("4. hash collision 처리와 load factor"),
            depthParagraph("""
서로 다른 key가 같은 bucket 후보를 얻을 수 있기 때문에 hash table은 chaining이나 open addressing 같은 collision 전략이 필요하다. 항목 수가 bucket에 비해 너무 많아지면 탐색 probe가 길어져 성능이 떨어진다. load factor가 임계값을 넘으면 table을 키우고 rehash하는 구현이 많다. 평균 O(1)은 좋은 hash 분포와 적절한 load factor를 전제로 한다.
"""),
            depthHeading("5. 메모리 비용은 “값 크기”보다 클 수 있다"),
            depthParagraph("""
linked list node는 값뿐 아니라 next/prev pointer, object header, allocator alignment 비용이 붙을 수 있다. hash table도 빈 bucket과 metadata가 필요하다. 수천만 항목에서는 원소당 16 byte 차이만 나도 수백 MB가 된다. 대규모 시스템은 알고리즘 복잡도와 함께 bytes per item을 실제 측정한다.
"""),
            depthCode("""
dynamic array capacity 예

capacity 4: [A B C D]
E append -> capacity 8로 확장하고 4개 복사
이후 F,G,H는 복사 없이 append
I append -> 다시 큰 확장

모든 append가 O(n) 복사를 하는 것이 아니다.
긴 연산열의 총 비용을 나눠 amortized cost를 본다.
"""),
            depthBullets(
                "Big-O 표에 “위치 reference가 이미 있는가” 같은 전제 조건을 함께 적는다.",
                "대규모 자료구조는 원소당 메모리와 cache miss를 측정한다.",
                "hash key의 equality와 hash가 저장 중 바뀌지 않는지 확인한다."
            )
        ),
        expertPack(
            sectionId = "V1-C03-S02",
            depthTitle("트리의 다음 층: 균형·디스크 페이지·우선순위·문자열 인덱스"),
            depthHeading("BST가 한쪽으로 기울면 왜 실전 DB 인덱스는 다른 트리를 쓰는가"),
            depthParagraph("""
단순 BST는 입력 순서에 따라 높이가 n까지 커질 수 있다. AVL과 Red-Black tree는 회전과 색/높이 규칙으로 높이를 O(log n)에 가깝게 유지한다. 하지만 DB와 파일 시스템은 메모리보다 디스크/SSD page I/O가 비싸기 때문에 한 node에 여러 key와 child를 담는 B-tree/B+tree 계열을 자주 사용한다. 한 번 page를 읽었을 때 많은 분기를 처리해 tree height와 I/O 횟수를 줄이는 목적이다.
"""),
            depthHeading("1. rotation은 값 순서를 깨지 않고 모양만 바꾼다"),
            depthParagraph("""
균형 BST의 rotation은 in-order 순서를 유지하면서 parent/child 연결만 바꾼다. 예를 들어 1→2→3으로 오른쪽에 기울어진 구조를 2를 중심으로 회전하면 1<2<3 정렬 관계는 그대로인데 높이는 줄어든다. “균형 트리는 자동으로 다시 정렬한다”가 아니라 검색 순서를 보존한 채 구조를 재배치한다.
"""),
            depthHeading("2. heap은 top 하나를 빠르게 유지한다"),
            depthParagraph("""
min-heap은 parent<=children 규칙만 보장하므로 root가 최솟값이라는 사실은 빠르게 알 수 있지만 전체 배열이 정렬된 것은 아니다. push/pop은 높이에 따라 O(log n)이며, priority queue에서 “다음으로 가장 중요한 작업 하나”를 계속 꺼낼 때 적합하다. 모든 항목을 완전 정렬해야 한다면 다른 알고리즘이 필요하다.
"""),
            depthHeading("3. trie의 비용은 문자열 길이와 alphabet 구조에 달린다"),
            depthParagraph("""
prefix search는 query 길이에 따라 경로를 내려간 뒤 subtree를 탐색할 수 있어 자동완성에 자연스럽다. 그러나 각 node가 문자별 child map을 가지면 짧은 단어가 수백만 개일 때 메모리 overhead가 매우 커질 수 있다. radix tree나 compressed trie는 한 자식만 이어지는 경로를 문자열 조각으로 합쳐 node 수를 줄인다.
"""),
            depthHeading("4. tree traversal 순서는 목적을 바꾼다"),
            depthParagraph("""
BST의 in-order traversal은 정렬 순서로 값을 얻는 데 쓰이고, pre-order는 구조 직렬화에, post-order는 자식 정리 후 부모를 처리하는 데 자연스럽다. “DFS 하나”라고만 외우기보다 node를 언제 처리하느냐에 따라 결과가 달라진다. 파일 삭제처럼 자식을 먼저 처리해야 안전한 작업도 있다.
"""),
            depthHeading("5. tree가 아닌데 tree처럼 보이는 데이터"),
            depthParagraph("""
파일 시스템의 symbolic link, 조직의 겸직, 카테고리의 다중 소속처럼 한 node가 여러 경로로 연결되면 순수 tree 가정이 깨진다. 같은 node를 여러 번 방문하거나 cycle이 생길 수 있으므로 graph 알고리즘과 visited가 필요하다. 모델을 먼저 잘못 고르면 좋은 tree 코드도 잘못된 결과를 낸다.
"""),
            depthCode("""
B+tree를 직관적으로 보기

[10 | 20 | 30]   <- 한 page에 여러 분기 key
 /     |     |  \
P0    P1    P2   P3

한 node마다 key 하나인 BST보다
한 번의 page read로 더 많은 범위를 좁힌다.
DB index가 “디스크/페이지 비용”을 의식하는 이유다.
"""),
            depthBullets(
                "자료가 메모리에만 있는지 storage page를 오가는지에 따라 tree 선택 기준이 달라진다.",
                "priority queue는 전체 정렬 자료구조가 아니다.",
                "계층처럼 보이는 데이터에 다중 부모나 link가 있으면 graph 가능성을 검사한다."
            )
        ),
        expertPack(
            sectionId = "V1-C03-S03",
            depthTitle("그래프를 진짜로 다루기: 저장 코드·메모리 계산·cycle·shortest path·DAG"),
            depthParagraph("""
V3에서 graph=node+edge와 BFS/DFS의 이름을 이미 봤다면 여기서는 실제 구현 결정으로 내려간다. 먼저 directed/undirected, weighted/unweighted, sparse/dense를 구분한다. 이 네 속성이 adjacency 저장 방식, 탐색 알고리즘, 메모리 크기, 최단 경로 알고리즘을 바꾼다. “그래프니까 visited를 쓴다” 수준을 넘어 어떤 상태를 언제 기록해야 하는지 손으로 추적한다.
"""),
            depthHeading("1. directed와 undirected는 저장 코드부터 다르다"),
            depthParagraph("""
undirected edge A—B를 adjacency list에 저장하면 보통 A 목록에 B, B 목록에 A를 둘 다 넣는다. directed A→B라면 A 목록에만 B를 넣는다. 이 차이를 놓치면 팔로우 관계를 친구 관계처럼 대칭으로 만들거나, dependency 방향을 거꾸로 탐색한다. reverse graph가 필요하면 별도 역방향 adjacency를 유지하기도 한다.
"""),
            depthHeading("2. weighted edge는 이웃만 저장하면 부족하다"),
            depthParagraph("""
지도에서 edge는 목적지뿐 아니라 거리·시간·비용 같은 weight를 가진다. adjacency list 항목을 Neighbor(to, weight)처럼 저장해야 한다. 여러 종류의 비용이 있으면 “최단”의 기준도 달라진다. 거리 최단과 시간 최단 경로가 다른 것은 알고리즘 문제가 아니라 먼저 weight 모델이 다르기 때문이다.
"""),
            depthHeading("3. adjacency list와 matrix의 메모리를 숫자로 비교한다"),
            depthParagraph("""
노드 100,000개와 edge 1,000,000개인 sparse graph에서 boolean matrix는 10^10개의 셀이 필요하다. 한 셀을 1 byte로만 잡아도 약 10GB다. adjacency list는 node header와 edge entry overhead가 있지만 O(V+E) 규모라 훨씬 작다. 반대로 node 수가 작고 연결 여부를 매우 자주 O(1)에 확인해야 하는 dense graph라면 matrix가 단순할 수 있다.
"""),
            depthHeading("4. visited를 “꺼낼 때” 넣으면 queue에 중복이 폭발할 수 있다"),
            depthParagraph("""
BFS에서 이웃을 발견해 queue에 넣을 때 visited 표시를 하지 않고, 나중에 dequeue할 때 표시하면 여러 부모가 같은 node를 동시에 발견해 queue에 반복 삽입할 수 있다. 결과가 맞더라도 메모리와 시간이 크게 늘 수 있다. 보통 unweighted BFS에서는 처음 발견해 enqueue하는 순간 visited/distance를 확정한다. 단, 최단 거리 알고리즘마다 “확정” 시점이 다르므로 규칙을 알고 사용한다.
"""),
            depthHeading("5. BFS가 최단 경로가 되는 조건"),
            depthParagraph("""
모든 edge 비용이 동일한 unweighted graph에서는 BFS가 시작점에서 edge 수가 적은 순서로 layer를 확장하므로 처음 발견한 거리가 최단이다. weight가 서로 다르면 단순 BFS는 틀릴 수 있다. 음수가 없는 가중치에서는 Dijkstra를 고려하고, 음수 edge가 있으면 Dijkstra의 “한 번 확정한 최단 거리는 더 줄지 않는다” 가정이 깨질 수 있어 Bellman-Ford 같은 다른 방법이 필요하다.
"""),
            depthHeading("6. DAG와 topological sort"),
            depthParagraph("""
dependency graph가 directed acyclic graph라면 “선행 작업이 먼저”가 되도록 topological order를 만들 수 있다. build dependency, 수강 선수과목, 작업 pipeline에 쓰인다. cycle이 존재하면 A가 B를 기다리고 B가 A를 기다리는 구조라 전체 순서를 만들 수 없다. indegree를 줄이는 Kahn 알고리즘이나 DFS 색상 상태로 cycle을 탐지할 수 있다.
"""),
            depthCode("""
weighted directed graph
A -> B (4)
A -> C (1)
C -> B (1)

BFS가 edge 개수만 보면 A->B 한 번이 가장 짧아 보인다.
하지만 weight 합은 4.
A->C->B는 edge 두 개지만 weight 합은 2.

가중치가 있으면 “짧다”의 정의부터 달라진다.
"""),
            depthBullets(
                "node 10만/edge 100만이면 저장 구조의 실제 메모리 추정부터 한다.",
                "BFS visited는 보통 enqueue 시 표시해 중복 삽입을 막는다.",
                "Dijkstra에 음수 weight를 넣기 전에 알고리즘의 전제부터 확인한다.",
                "dependency graph에 cycle이 있으면 topological order가 존재하지 않는다."
            )
        ),
        expertPack(
            sectionId = "V1-C03-S04",
            depthTitle("Big-O 다음 단계: amortized·공간·입력 분포·반례로 알고리즘을 선택한다"),
            depthHeading("복잡도는 하나의 문자로 끝나지 않는다"),
            depthParagraph("""
알고리즘을 비교할 때 worst-case time만 적으면 충분하지 않을 수 있다. 평균/기대 시간, amortized cost, extra space, 입력이 이미 정렬됐는지, 데이터가 cache에 들어가는지까지 고려한다. hash table 평균 O(1)과 최악 O(n), dynamic array append amortized O(1) 같은 표현은 “어떤 조건에서 어떤 비용을 말하는가”를 구체화한 것이다.
"""),
            depthHeading("1. 공간 복잡도는 대용량에서 시간보다 먼저 한계가 될 수 있다"),
            depthParagraph("""
BFS는 frontier와 visited를 저장하므로 폭이 큰 graph에서 메모리를 많이 쓴다. DFS는 깊이에 비례한 stack이 필요하지만 매우 깊은 graph에서는 recursion stack이 터질 수 있다. 알고리즘이 이론상 빠르더라도 메모리 한도를 넘으면 실행 자체가 불가능하므로 O(space)와 실제 bytes를 함께 본다.
"""),
            depthHeading("2. 정렬은 stable 여부와 data 특성까지 본다"),
            depthParagraph("""
사람 목록을 먼저 가입일로 정렬하고 다시 등급으로 정렬할 때 두 번째 정렬이 stable이면 같은 등급 안에서 가입일 순서가 보존될 수 있다. comparison sort의 O(n log n)만 비교하지 말고 안정성, in-place 여부, 거의 정렬된 입력에서의 동작을 확인한다. 표준 라이브러리 sort를 직접 구현보다 우선하되 요구사항에 필요한 보장을 문서에서 확인한다.
"""),
            depthHeading("3. greedy는 반례를 먼저 찾아본다"),
            depthParagraph("""
동전 1,3,4로 6을 만들 때 “가장 큰 동전부터” 고르면 4+1+1 세 개지만 최적은 3+3 두 개다. greedy가 맞으려면 local optimum이 global optimum으로 이어지는 교환 논리나 구조가 필요하다. 문제를 보고 “큰 것부터”라는 감각만 적용하지 말고 작은 반례를 손으로 찾는다.
"""),
            depthHeading("4. DP는 상태 정의가 절반이다"),
            depthParagraph("""
DP를 표 채우기 문법으로 외우면 새로운 문제에 적용하기 어렵다. “미래 결정을 위해 과거에서 무엇만 기억하면 되는가?”가 state 정의의 핵심이다. 배낭 문제라면 현재 item index와 남은 capacity처럼, 같은 state에 도달하면 이전 경로 세부를 버리고 최선 값만 재사용할 수 있어야 한다. state가 너무 크면 시간·메모리가 폭발한다.
"""),
            depthHeading("5. recursion을 iteration으로 바꾸는 이유는 stack을 직접 관리하기 위해서다"),
            depthParagraph("""
DFS 재귀는 코드가 짧지만 깊이가 100만인 chain graph에서는 call stack 한도를 넘길 수 있다. explicit stack 자료구조를 사용한 반복 구현은 같은 탐색 순서를 만들면서 메모리 위치와 오류 처리를 직접 제어할 수 있다. tail recursion 최적화가 없는 언어에서는 “꼬리 재귀니까 안전”하다고 가정하지 않는다.
"""),
            depthHeading("6. benchmark는 알고리즘 증명과 다른 도구다"),
            depthParagraph("""
Big-O는 입력 성장 형태를 설명하고 benchmark는 특정 하드웨어·런타임·데이터 분포에서 실제 시간을 측정한다. 둘 중 하나만으로 결론내리지 않는다. 작은 n에서는 단순 O(n) scan이 복잡한 index 구조보다 빠를 수 있고, 큰 n에서 역전될 수 있다. 실제 사용 범위를 대표하는 입력으로 warm-up, 반복, 변동성을 고려해 측정한다.
"""),
            depthCode("""
greedy 반례
동전 = [1, 3, 4], 목표 = 6

가장 큰 것부터:
4 + 1 + 1 = 3개

최적:
3 + 3 = 2개

“매 순간 가장 좋아 보이는 선택”이 전체 최적이라는 증거가 없다면
greedy는 후보일 뿐 정답이 아니다.
"""),
            depthBullets(
                "시간복잡도 옆에 공간복잡도와 중요한 전제를 같이 적는다.",
                "알고리즘을 바꾸기 전에 실제 n의 범위와 데이터 분포를 확인한다.",
                "DP는 점화식보다 state 정의와 중복 subproblem부터 찾는다.",
                "benchmark 결과는 측정 환경과 입력을 함께 기록한다."
            )
        )
    )
}

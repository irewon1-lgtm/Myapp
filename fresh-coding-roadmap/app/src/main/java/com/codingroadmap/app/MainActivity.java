package com.codingroadmap.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(247, 248, 250);
    private final int SURFACE = Color.WHITE;
    private final int BORDER = Color.rgb(226, 230, 236);
    private final int TEXT = Color.rgb(27, 31, 36);
    private final int MUTED = Color.rgb(95, 104, 115);
    private final int ACCENT = Color.rgb(44, 93, 160);
    private final int ACCENT_SOFT = Color.rgb(235, 242, 251);
    private final int CODE_BG = Color.rgb(24, 28, 34);
    private final int CODE_TEXT = Color.rgb(239, 242, 246);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applyWindowColors();
        setContentView(buildRoadmapScreen());
    }

    @Override
    public void onBackPressed() {
        setContentView(buildRoadmapScreen());
    }

    private void applyWindowColors() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    private View buildRoadmapScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(22), dp(18), dp(32));
        scroll.addView(root, matchWrap());

        root.addView(text("CODING ROADMAP", 12, ACCENT, Typeface.BOLD));

        TextView title = text("코딩을 처음부터\n제대로 배우는 순서", 30, TEXT, Typeface.BOLD);
        title.setLineSpacing(0f, 1.08f);
        root.addView(title, topMargin(wrapWrap(), dp(8)));

        root.addView(text("완전 초보 → 직접 앱 제작 → AI 활용 → 시스템 심화", 15, MUTED, Typeface.NORMAL),
                topMargin(wrapWrap(), dp(10)));

        TextView state = text("TRACK 01 내용 작성 완료  ·  나머지는 목차만", 13, ACCENT, Typeface.BOLD);
        state.setPadding(dp(12), dp(9), dp(12), dp(9));
        state.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 12));
        root.addView(state, topMargin(wrapWrap(), dp(14)));

        root.addView(text("트랙 1을 펼친 뒤 챕터를 누르면 학습 내용이 열립니다.", 13, MUTED, Typeface.NORMAL),
                topMargin(wrapWrap(), dp(16)));

        List<Lesson> track1Lessons = buildTrack1Lessons();
        for (Track track : buildTracks()) {
            root.addView(buildTrackCard(track, track1Lessons), topMargin(matchWrap(), dp(12)));
        }

        TextView footer = text("원칙 · 한 챕터 한 개념 / 짧은 설명 / 바로 따라하기 / 실제 결과 확인", 12, MUTED, Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(footer, topMargin(matchWrap(), dp(22)));
        return scroll;
    }

    private View buildTrackCard(Track track, List<Lesson> track1Lessons) {
        LinearLayout card = vertical();
        card.setPadding(dp(16), dp(15), dp(16), dp(15));
        card.setBackground(roundRect(SURFACE, BORDER, 18));
        card.setElevation(dp(1));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(head, matchWrap());

        LinearLayout titles = vertical();
        head.addView(titles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        String levelText = String.format("TRACK %02d  ·  %s", track.number, track.level);
        if (track.number == 1) levelText += "  ·  내용 완료";
        TextView label = text(levelText, 12, ACCENT, Typeface.BOLD);
        titles.addView(label);

        TextView title = text(track.title, 19, TEXT, Typeface.BOLD);
        title.setLineSpacing(0f, 1.08f);
        titles.addView(title, topMargin(wrapWrap(), dp(5)));

        TextView count = text(track.chapters.size() + "개  ▾", 13, ACCENT, Typeface.BOLD);
        count.setGravity(Gravity.CENTER);
        count.setPadding(dp(10), dp(8), dp(10), dp(8));
        count.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 12));
        head.addView(count);

        TextView goal = text(track.goal, 14, MUTED, Typeface.NORMAL);
        goal.setLineSpacing(0f, 1.18f);
        card.addView(goal, topMargin(matchWrap(), dp(10)));

        LinearLayout chapters = vertical();
        chapters.setVisibility(View.GONE);
        card.addView(chapters, topMargin(matchWrap(), dp(12)));

        for (int i = 0; i < track.chapters.size(); i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            TextView no = text(String.format("%02d", i + 1), 12, ACCENT, Typeface.BOLD);
            no.setMinWidth(dp(34));
            row.addView(no);

            TextView chapter = text(track.chapters.get(i), 15, TEXT, Typeface.NORMAL);
            chapter.setLineSpacing(0f, 1.15f);
            row.addView(chapter, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            if (track.number == 1 && i < track1Lessons.size()) {
                TextView open = text("열기 ›", 12, ACCENT, Typeface.BOLD);
                row.addView(open);
                final Lesson lesson = track1Lessons.get(i);
                row.setBackground(roundRect(Color.WHITE, Color.WHITE, 10));
                row.setOnClickListener(v -> setContentView(buildLessonScreen(lesson)));
            } else {
                TextView later = text("목차", 11, MUTED, Typeface.NORMAL);
                row.addView(later);
            }
            chapters.addView(row, matchWrap());
        }

        TextView milestone = text(track.milestone, 13, TEXT, Typeface.BOLD);
        milestone.setPadding(dp(12), dp(10), dp(12), dp(10));
        milestone.setBackground(roundRect(Color.rgb(245, 247, 250), Color.rgb(245, 247, 250), 12));
        chapters.addView(milestone, topMargin(matchWrap(), dp(7)));

        View.OnClickListener toggle = v -> {
            boolean open = chapters.getVisibility() == View.VISIBLE;
            chapters.setVisibility(open ? View.GONE : View.VISIBLE);
            count.setText(track.chapters.size() + (open ? "개  ▾" : "개  ▴"));
        };
        head.setOnClickListener(toggle);
        count.setOnClickListener(toggle);
        return card;
    }

    private View buildLessonScreen(Lesson lesson) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(18), dp(18), dp(36));
        scroll.addView(root, matchWrap());

        TextView back = text("‹  트랙 1 목차", 14, ACCENT, Typeface.BOLD);
        back.setPadding(0, dp(6), 0, dp(10));
        back.setOnClickListener(v -> setContentView(buildRoadmapScreen()));
        root.addView(back, wrapWrap());

        root.addView(text(String.format("TRACK 01  ·  CHAPTER %02d", lesson.number), 12, ACCENT, Typeface.BOLD),
                topMargin(wrapWrap(), dp(6)));

        TextView title = text(lesson.title, 28, TEXT, Typeface.BOLD);
        title.setLineSpacing(0f, 1.08f);
        root.addView(title, topMargin(matchWrap(), dp(7)));

        TextView pace = text("약 5분  ·  설명 1분 + 따라하기 3분 + 확인 1분", 13, MUTED, Typeface.NORMAL);
        root.addView(pace, topMargin(matchWrap(), dp(8)));

        root.addView(section("오늘 목표", lesson.goal, false), topMargin(matchWrap(), dp(18)));
        root.addView(section("딱 이것만 알기", lesson.explanation, false), topMargin(matchWrap(), dp(12)));
        root.addView(codeSection(lesson.code), topMargin(matchWrap(), dp(12)));
        root.addView(section("직접 해보기", lesson.practice, false), topMargin(matchWrap(), dp(12)));
        root.addView(section("예상 결과", lesson.expected, false), topMargin(matchWrap(), dp(12)));

        LinearLayout quiz = vertical();
        quiz.setPadding(dp(15), dp(14), dp(15), dp(14));
        quiz.setBackground(roundRect(SURFACE, BORDER, 16));
        quiz.addView(text("1문제 확인", 12, ACCENT, Typeface.BOLD));
        TextView q = text(lesson.question, 16, TEXT, Typeface.BOLD);
        q.setLineSpacing(0f, 1.15f);
        quiz.addView(q, topMargin(matchWrap(), dp(7)));

        TextView answer = text("정답 보기", 13, ACCENT, Typeface.BOLD);
        answer.setPadding(dp(11), dp(9), dp(11), dp(9));
        answer.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 10));
        quiz.addView(answer, topMargin(wrapWrap(), dp(10)));

        TextView answerBody = text(lesson.answer, 14, TEXT, Typeface.NORMAL);
        answerBody.setVisibility(View.GONE);
        answerBody.setLineSpacing(0f, 1.18f);
        quiz.addView(answerBody, topMargin(matchWrap(), dp(10)));

        answer.setOnClickListener(v -> {
            boolean hidden = answerBody.getVisibility() != View.VISIBLE;
            answerBody.setVisibility(hidden ? View.VISIBLE : View.GONE);
            answer.setText(hidden ? "정답 닫기" : "정답 보기");
        });
        root.addView(quiz, topMargin(matchWrap(), dp(12)));

        TextView finish = text(lesson.finish, 14, ACCENT, Typeface.BOLD);
        finish.setPadding(dp(14), dp(12), dp(14), dp(12));
        finish.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 14));
        root.addView(finish, topMargin(matchWrap(), dp(14)));

        return scroll;
    }

    private View section(String label, String body, boolean accent) {
        LinearLayout box = vertical();
        box.setPadding(dp(15), dp(14), dp(15), dp(14));
        box.setBackground(roundRect(accent ? ACCENT_SOFT : SURFACE, accent ? ACCENT_SOFT : BORDER, 16));
        box.addView(text(label, 12, ACCENT, Typeface.BOLD));
        TextView bodyView = text(body, 15, TEXT, Typeface.NORMAL);
        bodyView.setLineSpacing(0f, 1.2f);
        box.addView(bodyView, topMargin(matchWrap(), dp(7)));
        return box;
    }

    private View codeSection(String code) {
        LinearLayout box = vertical();
        box.setPadding(dp(15), dp(14), dp(15), dp(14));
        box.setBackground(roundRect(CODE_BG, CODE_BG, 16));
        box.addView(text("코드 보기", 12, Color.rgb(145, 185, 240), Typeface.BOLD));

        TextView codeView = text(code, 15, CODE_TEXT, Typeface.NORMAL);
        codeView.setTypeface(Typeface.MONOSPACE);
        codeView.setTextIsSelectable(true);
        codeView.setLineSpacing(0f, 1.18f);
        box.addView(codeView, topMargin(matchWrap(), dp(9)));
        return box;
    }

    private List<Lesson> buildTrack1Lessons() {
        List<Lesson> lessons = new ArrayList<>();

        lessons.add(new Lesson(
                1,
                "처음으로 코드를 실행해 보기",
                "코드를 읽기 전에 먼저 실행해서 결과가 나오는 경험을 만든다.",
                "코드는 컴퓨터에게 시킬 일을 적은 명령입니다. Python은 위에서 아래로 한 줄씩 읽습니다. 지금은 원리를 깊게 외우지 말고, 코드가 실행되면 화면에 결과가 나온다는 것만 확인하면 됩니다.",
                "print(\"안녕, 코딩!\")\nprint(2 + 3)",
                "① 첫 줄의 글자를 내 이름으로 바꿔 보세요.\n② 2 + 3을 10 + 7로 바꿔 보세요.\n③ 다시 실행해서 결과가 바뀌는지 확인하세요.",
                "안녕, 코딩!\n5\n\n숫자를 10 + 7로 바꾸면 17이 나옵니다.",
                "print(10 - 4)를 실행하면 무엇이 나올까요?",
                "6이 나옵니다. print 안의 계산이 먼저 되고, 그 결과를 화면에 보여줍니다.",
                "성공 기준 · 코드를 한 글자라도 직접 바꾸고 결과 변화까지 확인했다."
        ));

        lessons.add(new Lesson(
                2,
                "코드는 위에서 아래로 실행된다",
                "두 줄 이상의 코드가 어떤 순서로 움직이는지 눈으로 확인한다.",
                "Python은 특별한 지시가 없으면 첫 줄부터 마지막 줄까지 순서대로 실행합니다. 지금은 복잡한 제어 흐름보다 ‘작성한 순서가 실행 순서’라는 기본 감각만 잡습니다.",
                "print(\"첫 번째\")\nprint(\"두 번째\")\nprint(\"세 번째\")",
                "① 두 번째 줄과 세 번째 줄의 위치를 바꿔 보세요.\n② 실행 결과도 같은 순서로 바뀌는지 확인하세요.\n③ 한 줄을 지우고 다시 실행해 보세요.",
                "첫 번째\n두 번째\n세 번째",
                "세 줄 중 맨 위 줄을 맨 아래로 옮기면 가장 먼저 출력되는 것은 무엇일까요?",
                "원래 두 번째 줄이 가장 먼저 출력됩니다. Python은 현재 적혀 있는 순서를 따릅니다.",
                "성공 기준 · 코드 순서를 바꾸면 결과 순서도 바뀐다는 것을 확인했다."
        ));

        lessons.add(new Lesson(
                3,
                "값에 이름 붙이기 — 변수",
                "숫자나 글자를 변수에 담고 다시 꺼내 쓴다.",
                "변수는 값을 보관하는 이름표입니다. 오른쪽의 값을 왼쪽 이름에 저장한다고 생각하면 됩니다. 처음부터 메모리 구조를 외울 필요는 없습니다.",
                "name = \"승원\"\nage = 37\n\nprint(name)\nprint(age)",
                "① name을 내 이름으로 바꿔 보세요.\n② age를 원하는 숫자로 바꿔 보세요.\n③ print(age + 1)을 한 줄 추가해 보세요.",
                "승원\n37\n\nprint(age + 1)을 추가하면 38도 출력됩니다.",
                "price = 5000이라고 저장했다면 print(price)는 무엇을 출력할까요?",
                "5000을 출력합니다. 변수 price 안에 저장된 값을 꺼내 쓰기 때문입니다.",
                "성공 기준 · 변수 값을 바꾸고 출력 결과까지 직접 확인했다."
        ));

        lessons.add(new Lesson(
                4,
                "숫자·문자·참거짓 구분하기",
                "가장 자주 쓰는 세 종류의 값을 구분한다.",
                "처음에는 세 가지만 구분하면 충분합니다. 숫자는 계산에 쓰고, 문자열은 글자를 담고, 참거짓은 ‘맞다/아니다’를 표현합니다. 타입 이름을 전부 암기할 필요는 없습니다.",
                "count = 3\nmessage = \"안녕하세요\"\nis_ready = True\n\nprint(count)\nprint(message)\nprint(is_ready)",
                "① count를 10으로 바꿔 보세요.\n② message를 원하는 문장으로 바꿔 보세요.\n③ True를 False로 바꿔 출력 차이를 확인하세요.",
                "3\n안녕하세요\nTrue",
                "다음 중 글자인 것은 무엇일까요?  10   /   \"10\"",
                "\"10\"이 글자입니다. 따옴표로 감싼 값은 문자열입니다.",
                "성공 기준 · 숫자와 따옴표가 있는 글자의 차이를 설명할 수 있다."
        ));

        lessons.add(new Lesson(
                5,
                "입력받고 결과 보여주기",
                "사용자가 입력한 값을 변수에 저장하고 출력한다.",
                "input은 사용자에게 값을 받는 기능이고 print는 결과를 보여주는 기능입니다. 프로그램은 ‘입력 → 처리 → 출력’ 구조로 생각하면 훨씬 단순해집니다.",
                "name = input(\"이름을 입력하세요: \")\nprint(\"반가워요,\", name)",
                "① 실행하고 직접 이름을 입력해 보세요.\n② 안내 문구를 ‘닉네임을 입력하세요’로 바꿔 보세요.\n③ 마지막 출력 문구도 원하는 말로 바꿔 보세요.",
                "이름을 입력하세요: 승원\n반가워요, 승원",
                "input으로 받은 값을 나중에 다시 사용하려면 무엇이 필요할까요?",
                "변수에 저장하면 됩니다. 예: name = input(...)처럼 입력 결과에 이름을 붙입니다.",
                "성공 기준 · 내가 입력한 글자가 프로그램 결과에 다시 나타났다."
        ));

        lessons.add(new Lesson(
                6,
                "오류 메시지를 겁내지 않고 읽기",
                "오류가 나면 첫 번째로 볼 위치와 메시지를 찾는다.",
                "오류는 실패 판정이 아니라 ‘어디가 잘못됐는지 알려주는 정보’입니다. 초보 단계에서는 ① 어느 줄인지 ② 어떤 종류의 오류인지 ③ 방금 바꾼 부분이 무엇인지 세 가지만 확인합니다.",
                "print(\"안녕하세요\")\nprint(10 + 5)",
                "① 첫 줄의 마지막 따옴표 하나를 일부러 지우고 실행해 보세요.\n② 오류가 뜨는 것을 확인하세요.\n③ 따옴표를 다시 넣고 정상 실행되는지 확인하세요.",
                "정상 코드에서는\n안녕하세요\n15\n\n따옴표를 지우면 문장이 끝나지 않았다는 문법 오류가 납니다.",
                "오류가 난 직후 가장 먼저 확인할 것은 ‘내가 방금 바꾼 부분’일까요, 아니면 프로그램 전체를 처음부터 다시 쓸까요?",
                "방금 바꾼 부분부터 확인하는 것이 좋습니다. 원인을 좁히는 가장 빠른 방법입니다.",
                "성공 기준 · 일부러 오류를 만들고 직접 원상복구했다."
        ));

        lessons.add(new Lesson(
                7,
                "변수로 작은 계산기 만들기",
                "값을 변수에 담고 계산해서 결과를 출력한다.",
                "프로그램은 거창하지 않습니다. 값을 저장하고, 계산하고, 결과를 보여주는 것만으로도 실제 도구가 됩니다. 이번 챕터에서는 변수 세 개로 작은 계산기를 만듭니다.",
                "price = 1200\ncount = 3\ntotal = price * count\n\nprint(\"총 금액:\", total, \"원\")",
                "① price를 2500으로 바꿔 보세요.\n② count를 4로 바꿔 보세요.\n③ total 결과를 먼저 예상한 뒤 실행해서 맞는지 확인하세요.",
                "총 금액: 3600 원\n\nprice=2500, count=4라면 총 금액은 10000원입니다.",
                "price = 3000, count = 2라면 total은 얼마일까요?",
                "6000입니다. total = price * count이므로 3000 × 2를 계산합니다.",
                "성공 기준 · 실행 전에 결과를 예상하고 실제 결과와 비교했다."
        ));

        lessons.add(new Lesson(
                8,
                "첫 미니 프로젝트 — 오늘 지출 계산기",
                "지금까지 배운 입력·변수·계산·출력을 한 프로그램에 연결한다.",
                "새 개념을 더 넣지 않습니다. 앞에서 사용한 것만 조합합니다. 작은 프로그램 하나를 끝까지 완성하는 경험이 이번 트랙의 목표입니다.",
                "name = input(\"이름: \")\ncoffee = 4500\nlunch = 9000\ntotal = coffee + lunch\n\nprint(name, \"님의 오늘 지출은\", total, \"원입니다.\")",
                "① name에 직접 이름을 입력하세요.\n② coffee와 lunch 금액을 실제 오늘 지출로 바꿔 보세요.\n③ 항목 하나를 더 추가하고 total 계산에도 포함해 보세요.\n④ 실행 결과가 손계산과 같은지 확인하세요.",
                "이름: 승원\n승원 님의 오늘 지출은 13500 원입니다.",
                "새 변수 snack = 3000을 추가했다면 total 계산식에는 무엇을 더해야 할까요?",
                "total = coffee + lunch + snack처럼 snack을 계산식에 추가하면 됩니다.",
                "TRACK 01 완료 · 이제 변수·입력·출력·간단한 계산을 이용해 작은 프로그램을 만들 수 있다."
        ));

        return lessons;
    }

    private List<Track> buildTracks() {
        List<Track> t = new ArrayList<>();
        t.add(new Track(1, "입문", "코딩 시작 — 실행부터 변수까지", "설명보다 실행을 먼저 하며 코딩의 기본 감각을 만든다.",
                a("처음으로 코드를 실행해 보기", "코드는 위에서 아래로 실행된다", "값에 이름 붙이기 — 변수", "숫자·문자·참거짓 구분하기", "입력받고 결과 보여주기", "오류 메시지를 겁내지 않고 읽기", "변수로 작은 계산기 만들기", "첫 미니 프로젝트 — 오늘 지출 계산기"),
                "완료 목표 · 코드를 직접 바꾸고, 실행하고, 오류를 고쳐 작은 프로그램 하나를 완성한다."));
        t.add(new Track(2, "입문", "Python 기초 — 조건문부터 함수까지", "기본 문법을 실제 프로그램 흐름으로 연결한다.",
                a("조건문 if", "반복문 for·while", "함수 만들기", "리스트와 딕셔너리", "문자열 다루기", "파일 읽기와 쓰기", "예외 처리", "Python 자동화 미니 프로젝트"),
                "완료 목표 · 반복 작업을 자동화하는 작은 Python 프로그램을 만든다."));
        t.add(new Track(3, "기초", "문제 해결 — 코드를 짜기 전에 생각하는 법", "문제를 작은 단계로 나누고 검증하는 습관을 만든다.",
                a("문제를 입력·처리·출력으로 나누기", "순서도와 의사코드", "리스트·스택·큐", "집합과 딕셔너리", "검색과 정렬의 기본", "시간 복잡도 감 잡기", "테스트 케이스 만들기", "문제 해결 미니 프로젝트"),
                "완료 목표 · 문제를 쪼개고 스스로 풀이 순서를 설계한다."));
        t.add(new Track(4, "기초", "웹의 기본 — 화면과 인터넷이 연결되는 구조", "웹페이지와 서버가 어떻게 연결되는지 이해한다.",
                a("웹페이지가 열리는 전체 흐름", "HTML로 구조 만들기", "CSS로 화면 꾸미기", "브라우저 개발자 도구", "주소·도메인·URL", "HTTP 요청과 응답", "폼과 사용자 입력", "첫 웹페이지 미니 프로젝트"),
                "완료 목표 · 직접 만든 웹 화면을 브라우저에서 실행한다."));
        t.add(new Track(5, "기초", "TypeScript — 움직이는 웹앱 만들기", "JavaScript 실행 흐름과 타입을 함께 익힌다.",
                a("JavaScript와 TypeScript의 관계", "변수·함수·객체 다시 보기", "타입과 interface", "배열과 객체 다루기", "이벤트와 화면 상태", "비동기·Promise·async/await", "fetch로 API 호출하기", "TypeScript 미니 웹앱"),
                "완료 목표 · 사용자 입력과 API가 연결된 작은 웹앱을 만든다."));
        t.add(new Track(6, "기초", "SQL과 데이터 — 저장하고 다시 찾는 법", "앱의 데이터를 표 구조로 설계하고 직접 조회한다.",
                a("데이터베이스·테이블·행·열", "SELECT로 데이터 조회", "INSERT·UPDATE·DELETE", "WHERE·ORDER BY·GROUP BY", "JOIN으로 표 연결하기", "기본키·외래키와 데이터 설계", "인덱스와 조회 속도", "트랜잭션과 데이터 안전", "SQL 미니 프로젝트"),
                "완료 목표 · 앱에 필요한 데이터를 직접 설계하고 SQL로 다룬다."));
        t.add(new Track(7, "실전", "API와 백엔드 — 앱 뒤쪽 만들기", "요청을 받고 데이터베이스와 연결하는 서버를 만든다.",
                a("서버와 API의 역할", "route와 endpoint", "path·query·header·body", "입력 검증", "controller·service·repository", "데이터베이스 연결", "로그인과 권한의 기본", "오류 응답과 로그", "백엔드 API 미니 프로젝트"),
                "완료 목표 · 데이터 저장·조회가 되는 간단한 API를 만든다."));
        t.add(new Track(8, "실전", "Git·테스트·디버깅 — 망가뜨리지 않고 고치기", "변경을 기록하고 오류를 재현·검증하는 방법을 익힌다.",
                a("Git이 저장하는 상태", "commit으로 변경 기록하기", "branch와 merge", "충돌 해결과 되돌리기", "테스트의 기본", "오류 재현과 디버깅", "로그와 stack trace", "빌드·CI·배포의 기본"),
                "완료 목표 · 변경 기록과 테스트를 남기며 안전하게 앱을 수정한다."));
        t.add(new Track(9, "실전", "Android 앱 — Galaxy에서 직접 실행하기", "휴대폰에서 작동하는 Android 앱의 기본 구조를 만든다.",
                a("Android 앱의 전체 구조", "Kotlin 필수 문법", "화면 만들기", "상태와 사용자 이벤트", "화면 이동", "휴대폰 저장소 사용", "API와 네트워크 연결", "권한·백그라운드 작업", "APK 빌드와 설치", "Android 미니 앱"),
                "완료 목표 · 직접 만든 APK를 Galaxy에 설치해 실행한다."));
        t.add(new Track(10, "실전", "AI 활용 개발 — LLM을 앱 기능으로 넣기", "AI를 단순 채팅이 아니라 앱의 기능으로 연결한다.",
                a("LLM이 입력을 처리하는 기본 구조", "프롬프트와 context", "구조화된 출력", "AI API 연결", "embedding과 검색", "RAG의 기본", "tool calling", "agent 작업 흐름", "eval과 결과 검증", "AI 미니 앱"),
                "완료 목표 · AI 기능이 실제로 작동하는 작은 앱을 만든다."));
        t.add(new Track(11, "심화", "시스템 심화 — 운영·보안·성능까지", "앱이 커졌을 때 필요한 내부 구조와 운영 지식을 익힌다.",
                a("프로세스·메모리·운영체제", "네트워크와 연결", "인증·권한·보안", "동시성과 비동기 처리", "성능·병목·캐시", "아키텍처와 모듈 분리", "컨테이너와 클라우드", "배포·관측·장애 대응", "최종 통합 프로젝트 설계"),
                "완료 목표 · 작은 서비스를 설계하고 운영 관점까지 설명한다."));
        return t;
    }

    private List<String> a(String... xs) { return Arrays.asList(xs); }
    private LinearLayout vertical() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private TextView text(String s, int sp, int color, int style) { TextView v = new TextView(this); v.setText(s); v.setTextSize(sp); v.setTextColor(color); v.setTypeface(Typeface.create("sans", style)); return v; }
    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) { GradientDrawable d = new GradientDrawable(); d.setColor(fill); d.setCornerRadius(dp(radiusDp)); d.setStroke(dp(1), stroke); return d; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams wrapWrap() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams p, int top) { p.topMargin = top; return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static class Track {
        final int number;
        final String level;
        final String title;
        final String goal;
        final List<String> chapters;
        final String milestone;

        Track(int number, String level, String title, String goal, List<String> chapters, String milestone) {
            this.number = number;
            this.level = level;
            this.title = title;
            this.goal = goal;
            this.chapters = chapters;
            this.milestone = milestone;
        }
    }

    private static class Lesson {
        final int number;
        final String title;
        final String goal;
        final String explanation;
        final String code;
        final String practice;
        final String expected;
        final String question;
        final String answer;
        final String finish;

        Lesson(int number, String title, String goal, String explanation, String code, String practice,
               String expected, String question, String answer, String finish) {
            this.number = number;
            this.title = title;
            this.goal = goal;
            this.explanation = explanation;
            this.code = code;
            this.practice = practice;
            this.expected = expected;
            this.question = question;
            this.answer = answer;
            this.finish = finish;
        }
    }
}

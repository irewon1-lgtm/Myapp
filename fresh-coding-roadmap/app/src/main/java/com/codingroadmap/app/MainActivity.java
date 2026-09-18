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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildScreen());
    }

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout root = vertical();
        root.setPadding(dp(18), dp(22), dp(18), dp(32));
        scroll.addView(root, matchWrap());

        TextView eyebrow = text("CODING ROADMAP", 12, ACCENT, Typeface.BOLD);
        root.addView(eyebrow);

        TextView title = text("코딩을 처음부터\n제대로 배우는 순서", 30, TEXT, Typeface.BOLD);
        title.setLineSpacing(0f, 1.08f);
        root.addView(title, topMargin(wrapWrap(), dp(8)));

        TextView subtitle = text("완전 초보 → 직접 앱 제작 → AI 활용 → 시스템 심화", 15, MUTED, Typeface.NORMAL);
        root.addView(subtitle, topMargin(wrapWrap(), dp(10)));

        TextView state = text("현재 버전  ·  목차만 구성 / 본문 없음", 13, ACCENT, Typeface.BOLD);
        state.setPadding(dp(12), dp(9), dp(12), dp(9));
        state.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 12));
        root.addView(state, topMargin(wrapWrap(), dp(14)));

        TextView guide = text("트랙을 누르면 세부 목차가 펼쳐집니다.", 13, MUTED, Typeface.NORMAL);
        root.addView(guide, topMargin(wrapWrap(), dp(16)));

        for (Track track : buildTracks()) {
            root.addView(buildTrackCard(track), topMargin(matchWrap(), dp(12)));
        }

        TextView footer = text("본문 · 문제 · 복습 · 점수 · 프로젝트 기능은 목차 확정 후 추가", 12, MUTED, Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(footer, topMargin(matchWrap(), dp(22)));
        return scroll;
    }

    private View buildTrackCard(Track track) {
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

        TextView label = text(String.format("TRACK %02d  ·  %s", track.number, track.level), 12, ACCENT, Typeface.BOLD);
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
            row.setGravity(Gravity.TOP);
            row.setPadding(0, dp(7), 0, dp(7));

            TextView no = text(String.format("%02d", i + 1), 12, ACCENT, Typeface.BOLD);
            no.setMinWidth(dp(34));
            row.addView(no);

            TextView chapter = text(track.chapters.get(i), 15, TEXT, Typeface.NORMAL);
            chapter.setLineSpacing(0f, 1.15f);
            row.addView(chapter, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            chapters.addView(row, matchWrap());
        }

        TextView milestone = text(track.milestone, 13, TEXT, Typeface.BOLD);
        milestone.setPadding(dp(12), dp(10), dp(12), dp(10));
        milestone.setBackground(roundRect(Color.rgb(245,247,250), Color.rgb(245,247,250), 12));
        chapters.addView(milestone, topMargin(matchWrap(), dp(7)));

        View.OnClickListener toggle = v -> {
            boolean open = chapters.getVisibility() == View.VISIBLE;
            chapters.setVisibility(open ? View.GONE : View.VISIBLE);
            count.setText(track.chapters.size() + (open ? "개  ▾" : "개  ▴"));
        };
        card.setOnClickListener(toggle);
        count.setOnClickListener(toggle);
        return card;
    }

    private List<Track> buildTracks() {
        List<Track> t = new ArrayList<>();
        t.add(new Track(1, "입문", "코딩 시작 — 실행부터 변수까지", "코드가 무엇인지 알고 직접 실행해 본다.",
                a("컴퓨터가 코드를 실행하는 전체 그림", "개발 도구와 실행 버튼", "값과 변수", "숫자·문자·참거짓", "입력과 출력", "오류 메시지 읽는 법", "작은 계산 프로그램", "첫 미니 프로젝트"),
                "완료 목표 · 코드를 직접 실행하고 간단한 프로그램을 만든다."));
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
        final int number; final String level; final String title; final String goal; final List<String> chapters; final String milestone;
        Track(int number, String level, String title, String goal, List<String> chapters, String milestone) {
            this.number = number; this.level = level; this.title = title; this.goal = goal; this.chapters = chapters; this.milestone = milestone;
        }
    }
}

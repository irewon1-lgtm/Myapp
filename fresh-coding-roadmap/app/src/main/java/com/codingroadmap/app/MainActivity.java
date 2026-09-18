package com.codingroadmap.app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private final int BG = Color.rgb(11, 12, 15);
    private final int SURFACE = Color.rgb(17, 19, 24);
    private final int BORDER = Color.rgb(48, 52, 60);
    private final int TEXT = Color.rgb(243, 241, 234);
    private final int MUTED = Color.rgb(173, 178, 188);
    private final int ACCENT = Color.rgb(138, 180, 248);
    private final int ACCENT_SOFT = Color.rgb(30, 53, 83);
    private final int CODE_BG = Color.rgb(7, 9, 12);
    private final int CODE_TEXT = Color.rgb(233, 237, 243);

    private SharedPreferences prefs;
    private List<Track1Content.Page> track1Pages;
    private List<Track1Content.Page> track2Pages;
    private List<Track1Content.Page> activePages;
    private int activeTrackNumber = 1;
    private boolean inReader = false;

    private int readerPageIndex = 0;
    private FrameLayout readerPageHolder;
    private TextView readerChapterText;
    private TextView readerLessonText;
    private TextView readerPageText;
    private TextView readerPrevText;
    private TextView readerNextText;
    private boolean readerAnimating = false;
    private float swipeDownX = 0f;
    private float swipeDownY = 0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("fresh_coding_roadmap", MODE_PRIVATE);
        track1Pages = Track1Content.pages();
        track2Pages = Track2Content.pages();
        activePages = track1Pages;
        applyWindowColors();
        showRoadmap();
    }

    @Override
    public void onBackPressed() {
        if (inReader) {
            showRoadmap();
        } else {
            super.onBackPressed();
        }
    }

    private void applyWindowColors() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);
    }

    private void showRoadmap() {
        inReader = false;
        setContentView(buildRoadmapScreen());
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

        TextView state = text(
                "TRACK 01  ·  " + track1Pages.size() + "장 완료\n"
                        + "TRACK 02  ·  " + track2Pages.size() + "장 완료",
                13, ACCENT, Typeface.BOLD);
        state.setLineSpacing(0f, 1.25f);
        state.setPadding(dp(12), dp(9), dp(12), dp(9));
        state.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 12));
        root.addView(state, topMargin(wrapWrap(), dp(14)));

        root.addView(text("TRACK 01~02는 각 트랙 안에서 좌우로 계속 넘기며 읽을 수 있습니다.", 13, MUTED, Typeface.NORMAL),
                topMargin(wrapWrap(), dp(16)));

        for (Track track : buildTracks()) {
            root.addView(buildTrackCard(track), topMargin(matchWrap(), dp(12)));
        }

        TextView footer = text("TRACK 03~11은 아직 목차만 있습니다.", 12, MUTED, Typeface.NORMAL);
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

        String labelText = String.format("TRACK %02d  ·  %s", track.number, track.level);
        if (track.number <= 2) labelText += "  ·  내용 완료";
        titles.addView(text(labelText, 12, ACCENT, Typeface.BOLD));

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
            row.setPadding(0, dp(9), 0, dp(9));

            TextView no = text(String.format("%02d", i + 1), 12, ACCENT, Typeface.BOLD);
            no.setMinWidth(dp(34));
            row.addView(no);

            TextView chapter = text(track.chapters.get(i), 15, TEXT, Typeface.NORMAL);
            chapter.setLineSpacing(0f, 1.15f);
            row.addView(chapter, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            if (track.number <= 2) {
                TextView open = text("바로가기 ›", 11, ACCENT, Typeface.BOLD);
                row.addView(open);
                final int lessonNumber = i + 1;
                final int trackNumber = track.number;
                row.setOnClickListener(v -> {
                    List<Track1Content.Page> pages = pagesForTrack(trackNumber);
                    int firstPage = trackNumber == 1
                            ? Track1Content.firstPageIndexOfLesson(pages, lessonNumber)
                            : Track2Content.firstPageIndexOfLesson(pages, lessonNumber);
                    openTrackReader(trackNumber, firstPage);
                });
            } else {
                row.addView(text("목차", 11, MUTED, Typeface.NORMAL));
            }
            chapters.addView(row, matchWrap());
        }

        TextView milestone = text(track.milestone, 13, TEXT, Typeface.BOLD);
        milestone.setPadding(dp(12), dp(10), dp(12), dp(10));
        milestone.setBackground(roundRect(Color.rgb(29, 32, 38), Color.rgb(29, 32, 38), 12));
        chapters.addView(milestone, topMargin(matchWrap(), dp(7)));

        if (track.number <= 2) {
            LinearLayout actions = new LinearLayout(this);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER_VERTICAL);
            final int trackNumber = track.number;

            TextView start = actionButton("처음부터");
            start.setOnClickListener(v -> openTrackReader(trackNumber, 0));
            actions.addView(start, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            LinearLayout.LayoutParams gap = new LinearLayout.LayoutParams(dp(10), 1);
            actions.addView(new View(this), gap);

            TextView resume = actionButton("이어보기");
            resume.setOnClickListener(v -> {
                int saved = prefs.getInt(readerPreferenceKey(trackNumber), 0);
                openTrackReader(trackNumber, saved);
            });
            actions.addView(resume, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            chapters.addView(actions, topMargin(matchWrap(), dp(12)));
        }

        View.OnClickListener toggle = v -> {
            boolean open = chapters.getVisibility() == View.VISIBLE;
            chapters.setVisibility(open ? View.GONE : View.VISIBLE);
            count.setText(track.chapters.size() + (open ? "개  ▾" : "개  ▴"));
        };
        head.setOnClickListener(toggle);
        count.setOnClickListener(toggle);
        return card;
    }

    private TextView actionButton(String label) {
        TextView button = text(label, 14, ACCENT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(12), dp(11), dp(12), dp(11));
        button.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 12));
        return button;
    }

    private List<Track1Content.Page> pagesForTrack(int trackNumber) {
        if (trackNumber == 2) return track2Pages;
        return track1Pages;
    }

    private String readerPreferenceKey(int trackNumber) {
        return "track" + trackNumber + "_reader_page";
    }

    private void openTrackReader(int trackNumber, int requestedPage) {
        inReader = true;
        activeTrackNumber = trackNumber;
        activePages = pagesForTrack(trackNumber);
        readerPageIndex = Math.max(0, Math.min(requestedPage, activePages.size() - 1));
        prefs.edit().putInt(readerPreferenceKey(trackNumber), readerPageIndex).apply();
        setContentView(buildReaderShell());
    }

    private View buildReaderShell() {
        LinearLayout root = vertical();
        root.setBackgroundColor(BG);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(8), dp(7), dp(8), dp(7));
        top.setBackgroundColor(SURFACE);

        TextView toc = text("‹ 목차", 14, ACCENT, Typeface.BOLD);
        toc.setPadding(dp(8), dp(8), dp(8), dp(8));
        toc.setOnClickListener(v -> showRoadmap());
        top.addView(toc, wrapWrap());

        LinearLayout topCenter = vertical();
        topCenter.setGravity(Gravity.CENTER);
        top.addView(topCenter, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        readerChapterText = text("", 11, ACCENT, Typeface.BOLD);
        readerChapterText.setGravity(Gravity.CENTER);
        topCenter.addView(readerChapterText, matchWrap());

        readerLessonText = text("", 13, TEXT, Typeface.BOLD);
        readerLessonText.setGravity(Gravity.CENTER);
        readerLessonText.setMaxLines(1);
        topCenter.addView(readerLessonText, topMargin(matchWrap(), dp(2)));

        TextView save = text("자동저장", 11, MUTED, Typeface.BOLD);
        save.setGravity(Gravity.CENTER);
        save.setPadding(dp(8), dp(8), dp(8), dp(8));
        top.addView(save, wrapWrap());

        root.addView(top, matchWrap());

        FrameLayout readerArea = new FrameLayout(this);
        readerArea.setBackgroundColor(BG);
        root.addView(readerArea, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

        readerPageHolder = new FrameLayout(this);
        readerArea.addView(readerPageHolder, matchMatch());
        readerPageHolder.addView(buildReaderPageView(activePages.get(readerPageIndex)), matchMatch());

        TextView leftZone = edgeZone("‹");
        FrameLayout.LayoutParams leftParams = new FrameLayout.LayoutParams(dp(48), FrameLayout.LayoutParams.MATCH_PARENT);
        leftParams.gravity = Gravity.START;
        readerArea.addView(leftZone, leftParams);
        leftZone.setOnClickListener(v -> previousReaderPage());

        TextView rightZone = edgeZone("›");
        FrameLayout.LayoutParams rightParams = new FrameLayout.LayoutParams(dp(48), FrameLayout.LayoutParams.MATCH_PARENT);
        rightParams.gravity = Gravity.END;
        readerArea.addView(rightZone, rightParams);
        rightZone.setOnClickListener(v -> nextReaderPage());

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(dp(12), dp(8), dp(12), dp(10));
        bottom.setBackgroundColor(SURFACE);

        readerPrevText = text("‹ 이전", 13, ACCENT, Typeface.BOLD);
        readerPrevText.setPadding(dp(8), dp(8), dp(8), dp(8));
        readerPrevText.setOnClickListener(v -> previousReaderPage());
        bottom.addView(readerPrevText, wrapWrap());

        readerPageText = text("", 12, MUTED, Typeface.BOLD);
        readerPageText.setGravity(Gravity.CENTER);
        bottom.addView(readerPageText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        readerNextText = text("다음 ›", 13, ACCENT, Typeface.BOLD);
        readerNextText.setPadding(dp(8), dp(8), dp(8), dp(8));
        readerNextText.setOnClickListener(v -> nextReaderPage());
        bottom.addView(readerNextText, wrapWrap());

        root.addView(bottom, matchWrap());

        updateReaderChrome();
        return root;
    }

    private TextView edgeZone(String arrow) {
        TextView zone = text(arrow, 28, Color.argb(150, 138, 180, 248), Typeface.BOLD);
        zone.setGravity(Gravity.CENTER);
        return zone;
    }

    private View buildReaderPageView(Track1Content.Page page) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        installSwipeNavigation(scroll);

        LinearLayout outer = vertical();
        outer.setGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        outer.setPadding(dp(isTablet() ? 28 : 18), dp(14), dp(isTablet() ? 28 : 18), dp(22));
        scroll.addView(outer, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));

        LinearLayout paper = vertical();
        paper.setPadding(dp(isTablet() ? 30 : 22), dp(isTablet() ? 26 : 20),
                dp(isTablet() ? 30 : 22), dp(isTablet() ? 30 : 24));
        paper.setBackground(roundRect(SURFACE, BORDER, 18));
        paper.setElevation(dp(1));
        paper.setMinimumHeight(Math.max(dp(520),
                getResources().getDisplayMetrics().heightPixels - dp(isTablet() ? 175 : 165)));

        LinearLayout.LayoutParams paperParams = matchWrap();
        if (isTablet()) {
            int available = getResources().getDisplayMetrics().widthPixels - dp(56);
            paperParams.width = Math.min(available, dp(780));
            paperParams.gravity = Gravity.CENTER_HORIZONTAL;
        }
        outer.addView(paper, paperParams);

        TextView kicker = text(
                String.format("CHAPTER %02d  ·  %s", page.lessonNumber, page.kind),
                12, ACCENT, Typeface.BOLD);
        paper.addView(kicker);

        TextView title = text(page.title, isTablet() ? 30 : 26, TEXT, Typeface.BOLD);
        title.setLineSpacing(0f, 1.1f);
        paper.addView(title, topMargin(matchWrap(), dp(8)));

        TextView lessonTitle = text(page.lessonTitle, isTablet() ? 15 : 13, MUTED, Typeface.NORMAL);
        lessonTitle.setLineSpacing(0f, 1.15f);
        paper.addView(lessonTitle, topMargin(matchWrap(), dp(7)));

        TextView body = text(page.body, isTablet() ? 18 : 16, TEXT, Typeface.NORMAL);
        body.setLineSpacing(0f, isTablet() ? 1.32f : 1.25f);
        paper.addView(body, topMargin(matchWrap(), dp(18)));

        if (page.hasEasyExplanation()) {
            paper.addView(buildEasyExplanationBox(page.easyExplanation), topMargin(matchWrap(), dp(16)));
        }

        if (page.hasCode()) {
            paper.addView(buildCodeBox(page.code), topMargin(matchWrap(), dp(16)));
        }

        if (page.hasPractice()) {
            paper.addView(buildPracticeBox(page.practice), topMargin(matchWrap(), dp(16)));
        }

        if (page.hasQuestion()) {
            paper.addView(buildQuestionBox(page.question), topMargin(matchWrap(), dp(16)));
        }

        if (page.hasKeywords()) {
            LinearLayout keywords = vertical();
            keywords.setPadding(dp(13), dp(11), dp(13), dp(11));
            keywords.setBackground(roundRect(ACCENT_SOFT, ACCENT_SOFT, 12));
            keywords.addView(text("찾아볼 수 있어야 하는 단어", 11, ACCENT, Typeface.BOLD));
            TextView keywordBody = text(page.keywords, 13, TEXT, Typeface.NORMAL);
            keywordBody.setLineSpacing(0f, 1.18f);
            keywords.addView(keywordBody, topMargin(matchWrap(), dp(5)));
            paper.addView(keywords, topMargin(matchWrap(), dp(16)));
        }

        TextView hint = text("오른쪽 끝을 누르거나 왼쪽으로 밀면 다음 장", 11, MUTED, Typeface.NORMAL);
        hint.setGravity(Gravity.CENTER);
        paper.addView(hint, topMargin(matchWrap(), dp(18)));

        return scroll;
    }

    private View buildEasyExplanationBox(String explanation) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(13), dp(14), dp(14));
        box.setBackground(roundRect(Color.rgb(42, 36, 24), Color.rgb(95, 76, 39), 14));

        box.addView(text("이게 무슨 뜻이야? — 더 쉽게 설명", 12, Color.rgb(236, 199, 118), Typeface.BOLD));

        TextView body = text(explanation, isTablet() ? 17 : 15, TEXT, Typeface.NORMAL);
        body.setLineSpacing(0f, isTablet() ? 1.30f : 1.25f);
        box.addView(body, topMargin(matchWrap(), dp(8)));
        return box;
    }

    private View buildCodeBox(String code) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(13), dp(14), dp(14));
        box.setBackground(roundRect(CODE_BG, CODE_BG, 14));

        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        box.addView(head, matchWrap());

        TextView label = text("PYTHON", 11, Color.rgb(145, 185, 240), Typeface.BOLD);
        head.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView copy = text("복사", 12, Color.rgb(180, 205, 240), Typeface.BOLD);
        copy.setPadding(dp(8), dp(5), dp(8), dp(5));
        copy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("python code", code));
            copy.setText("복사됨");
        });
        head.addView(copy, wrapWrap());

        TextView codeText = text(code, isTablet() ? 17 : 15, CODE_TEXT, Typeface.NORMAL);
        codeText.setTypeface(Typeface.MONOSPACE);
        codeText.setTextIsSelectable(true);
        codeText.setLineSpacing(0f, 1.18f);
        box.addView(codeText, topMargin(matchWrap(), dp(9)));
        return box;
    }

    private View buildPracticeBox(String practice) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(13), dp(14), dp(14));
        box.setBackground(roundRect(Color.rgb(23, 26, 32), BORDER, 14));
        box.addView(text("직접 해보기", 12, ACCENT, Typeface.BOLD));

        TextView body = text(practice, isTablet() ? 17 : 15, TEXT, Typeface.NORMAL);
        body.setLineSpacing(0f, isTablet() ? 1.28f : 1.22f);
        box.addView(body, topMargin(matchWrap(), dp(7)));

        TextView answerNotice = text("→ 다음 장: 예시 정답 + 한 단계씩 풀이", 12, ACCENT, Typeface.BOLD);
        answerNotice.setPadding(0, dp(10), 0, 0);
        box.addView(answerNotice);
        return box;
    }

    private View buildQuestionBox(String question) {
        LinearLayout box = vertical();
        box.setPadding(dp(14), dp(13), dp(14), dp(14));
        box.setBackground(roundRect(SURFACE, BORDER, 14));
        box.addView(text("확인 문제", 12, ACCENT, Typeface.BOLD));

        TextView q = text(question, isTablet() ? 17 : 15, TEXT, Typeface.BOLD);
        q.setLineSpacing(0f, isTablet() ? 1.28f : 1.22f);
        box.addView(q, topMargin(matchWrap(), dp(7)));

        TextView answerNotice = text("정답은 바로 다음 장에서 아주 쉽게 풀이합니다.", 12, ACCENT, Typeface.BOLD);
        answerNotice.setPadding(0, dp(10), 0, 0);
        box.addView(answerNotice);
        return box;
    }

    private void installSwipeNavigation(View target) {
        final float threshold = dp(72);
        target.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    swipeDownX = event.getX();
                    swipeDownY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - swipeDownX;
                    float dy = event.getY() - swipeDownY;
                    if (Math.abs(dx) >= threshold && Math.abs(dx) > Math.abs(dy) * 1.25f) {
                        if (dx < 0) nextReaderPage();
                        else previousReaderPage();
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    swipeDownX = 0f;
                    swipeDownY = 0f;
                    break;
            }
            return false;
        });
    }

    private void nextReaderPage() {
        if (readerPageIndex < activePages.size() - 1) {
            changeReaderPage(readerPageIndex + 1, 1);
        }
    }

    private void previousReaderPage() {
        if (readerPageIndex > 0) {
            changeReaderPage(readerPageIndex - 1, -1);
        }
    }

    private void changeReaderPage(int targetIndex, int direction) {
        if (readerAnimating || readerPageHolder == null) return;
        targetIndex = Math.max(0, Math.min(targetIndex, activePages.size() - 1));
        if (targetIndex == readerPageIndex) return;

        readerAnimating = true;
        View oldPage = readerPageHolder.getChildCount() > 0
                ? readerPageHolder.getChildAt(readerPageHolder.getChildCount() - 1)
                : null;
        View newPage = buildReaderPageView(activePages.get(targetIndex));

        int width = readerPageHolder.getWidth();
        if (width <= 0 || oldPage == null) {
            readerPageHolder.removeAllViews();
            readerPageHolder.addView(newPage, matchMatch());
            readerPageIndex = targetIndex;
            saveReaderPosition();
            updateReaderChrome();
            readerAnimating = false;
            return;
        }

        newPage.setTranslationX(direction > 0 ? width : -width);
        newPage.setAlpha(0.85f);
        readerPageHolder.addView(newPage, matchMatch());

        oldPage.animate()
                .translationX(direction > 0 ? -width * 0.24f : width * 0.24f)
                .alpha(0f)
                .setDuration(170)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        readerPageHolder.removeView(oldPage);
                    }
                })
                .start();

        newPage.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(190)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        readerAnimating = false;
                    }
                })
                .start();

        readerPageIndex = targetIndex;
        saveReaderPosition();
        updateReaderChrome();
    }

    private void saveReaderPosition() {
        prefs.edit().putInt(readerPreferenceKey(activeTrackNumber), readerPageIndex).apply();
    }

    private void updateReaderChrome() {
        if (activePages == null || activePages.isEmpty()) return;
        Track1Content.Page page = activePages.get(readerPageIndex);

        if (readerChapterText != null) {
            readerChapterText.setText(String.format("TRACK %02d  ·  CHAPTER %02d", activeTrackNumber, page.lessonNumber));
        }
        if (readerLessonText != null) {
            readerLessonText.setText(page.lessonTitle);
        }
        if (readerPageText != null) {
            readerPageText.setText((readerPageIndex + 1) + " / " + activePages.size());
        }
        if (readerPrevText != null) {
            boolean enabled = readerPageIndex > 0;
            readerPrevText.setAlpha(enabled ? 1f : 0.28f);
        }
        if (readerNextText != null) {
            boolean enabled = readerPageIndex < activePages.size() - 1;
            readerNextText.setAlpha(enabled ? 1f : 0.28f);
            readerNextText.setText(enabled ? "다음 ›" : String.format("TRACK %02d 완료", activeTrackNumber));
        }
    }

    private List<Track> buildTracks() {
        List<Track> t = new ArrayList<>();
        t.add(new Track(1, "입문", "코딩 시작 — 실행부터 작은 프로그램까지", "아무것도 모르는 상태에서 용어·원리·실습을 함께 배운다.",
                a("코딩이 무엇인지 알고 첫 코드를 실행하기", "코드가 위에서 아래로 실행되는 흐름 이해하기", "값에 이름을 붙이는 변수 이해하기", "숫자·문자열·참거짓과 데이터 타입 이해하기", "입력과 출력, 형 변환 이해하기", "오류 메시지와 디버깅의 기본 이해하기", "연산자와 계산 순서로 작은 계산기 만들기", "첫 미니 프로젝트를 설계하고 테스트하기"),
                "완료 목표 · 기본 코딩 용어를 이해하고, 검색한 설명을 읽으며 간단한 Python 프로그램을 만들고 고칠 수 있다."));
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

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private TextView text(String s, int sp, int color, int style) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans", style));
        return v;
    }

    private GradientDrawable roundRect(int fill, int stroke, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private FrameLayout.LayoutParams matchMatch() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
    }

    private LinearLayout.LayoutParams topMargin(LinearLayout.LayoutParams p, int top) {
        p.topMargin = top;
        return p;
    }

    private boolean isTablet() {
        return getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

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
}

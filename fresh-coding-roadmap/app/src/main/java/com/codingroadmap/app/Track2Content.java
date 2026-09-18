package com.codingroadmap.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Track2Content {
    private Track2Content() {}

    static List<Track1Content.Page> pages() {
        List<Track1Content.Page> out = new ArrayList<>();
        Track2Lesson01.append(out);
        Track2Lesson02.append(out);
        Track2Lesson03.append(out);
        Track2Lesson04.append(out);
        Track2Lesson05.append(out);
        Track2Lesson06.append(out);
        Track2Lesson07.append(out);
        Track2Lesson08.append(out);
        return Collections.unmodifiableList(out);
    }

    static Track1Content.Page page(
            int lesson,
            String lessonTitle,
            String kind,
            String title,
            String body,
            String easyExplanation,
            String code,
            String practice,
            String question,
            String answer,
            String keywords
    ) {
        return new Track1Content.Page(
                lesson,
                lessonTitle,
                kind,
                title,
                body,
                easyExplanation == null ? "" : easyExplanation,
                code,
                practice,
                question,
                answer,
                keywords
        );
    }

    static Track1Content.Page vocab(
            int lesson,
            String lessonTitle,
            String title,
            String body
    ) {
        return page(
                lesson,
                lessonTitle,
                "용어집",
                title,
                body,
                "",
                null,
                null,
                null,
                null,
                "용어 뜻 → 어디에 쓰는지 → 실제 예 순서로 읽기"
        );
    }

    static Track1Content.Page practiceAnswer(
            int lesson,
            String lessonTitle,
            String sourceTitle,
            String body,
            String code
    ) {
        return page(
                lesson,
                lessonTitle,
                "실습 정답·해설",
                sourceTitle + " — 예시 정답과 아주 쉬운 풀이",
                body,
                "",
                code,
                null,
                null,
                null,
                "정답을 복사하기보다 왜 이렇게 되는지 한 줄씩 확인하기"
        );
    }

    static Track1Content.Page questionAnswer(
            int lesson,
            String lessonTitle,
            String sourceTitle,
            String answer,
            String explanation,
            String application,
            String mistakeGuide
    ) {
        String body = "정답\n" + answer
                + "\n\n왜 이 답이 되는지 한 단계씩\n" + explanation
                + "\n\n한 번 더 적용해 보기\n" + application
                + "\n\n자주 틀리는 이유\n" + mistakeGuide;
        return page(
                lesson,
                lessonTitle,
                "문제 정답·해설",
                sourceTitle + " — 정답 + 단계별 해설 + 응용",
                body,
                "",
                null,
                null,
                null,
                null,
                "문제를 다시 보고 자기 말로 한 번 설명하면 더 잘 기억됩니다."
        );
    }

    static int firstPageIndexOfLesson(List<Track1Content.Page> pages, int lessonNumber) {
        for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).lessonNumber == lessonNumber) return i;
        }
        return 0;
    }
}

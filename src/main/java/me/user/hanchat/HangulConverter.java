package me.user.hanchat;

import java.util.HashMap;
import java.util.Map;

public class HangulConverter {
    // 매핑 수정: P(ㅖ), O(ㅒ) 순서로 자판 배열에 맞게 조정
    private static final String ENG_KEY = "q w e r t y u i o p a s d f g h j k l z x c v b n m Q W E R T P O";
    private static final String KOR_KEY = "ㅂ ㅈ ㄷ ㄱ ㅅ ㅛ ㅕ ㅑ ㅐ ㅔ ㅁ ㄴ ㅇ ㄹ ㅎ ㅗ ㅓ ㅏ ㅣ ㅋ ㅌ ㅊ ㅍ ㅠ ㅜ ㅡ ㅃ ㅉ ㄸ ㄲ ㅆ ㅖ ㅒ";
    private static final Map<Character, String> KEY_MAP = new HashMap<>();
    private static final Map<String, String> DOUBLE_JUNG = new HashMap<>();
    private static final Map<String, String> DOUBLE_JONG = new HashMap<>();

    private static final String CHO = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";
    private static final String JUNG = "ㅏㅐㅑㅒㅓㅔㅕㅖㅗㅘㅙㅚㅛㅜㅝㅞㅟㅠㅡㅢㅣ";
    private static final String JONG = "ㄱㄲㄳㄴㄵㄶㄷㄹㄺㄻㄼㄽㄾㄿㅀㅁㅂㅄㅅㅆㅇㅈㅊㅋㅌㅍㅎ";

    static {
        String[] e = ENG_KEY.split(" ");
        String[] k = KOR_KEY.split(" ");
        for (int i = 0; i < e.length; i++) {
            if (i < k.length) KEY_MAP.put(e[i].charAt(0), k[i]);
        }

        // 이중 모음 (왜, 워 등)
        DOUBLE_JUNG.put("ㅗㅏ", "ㅘ"); DOUBLE_JUNG.put("ㅗㅐ", "ㅙ"); DOUBLE_JUNG.put("ㅗㅣ", "ㅚ");
        DOUBLE_JUNG.put("ㅜㅓ", "ㅝ"); DOUBLE_JUNG.put("ㅜㅔ", "ㅞ"); DOUBLE_JUNG.put("ㅜㅣ", "ㅟ");
        DOUBLE_JUNG.put("ㅡㅣ", "ㅢ");

        // 겹받침 (ㅄ, ㄺ 등 모든 조합)
        DOUBLE_JONG.put("ㄱㅅ", "ㄳ"); DOUBLE_JONG.put("ㄴㅈ", "ㄵ"); DOUBLE_JONG.put("ㄴㅎ", "ㄶ");
        DOUBLE_JONG.put("ㄹㄱ", "ㄺ"); DOUBLE_JONG.put("ㄹㅁ", "ㄻ"); DOUBLE_JONG.put("ㄹㅂ", "ㄼ");
        DOUBLE_JONG.put("ㄹㅅ", "ㄽ"); DOUBLE_JONG.put("ㄹㅌ", "ㄾ"); DOUBLE_JONG.put("ㄹㅍ", "ㄿ");
        DOUBLE_JONG.put("ㄹㅎ", "ㅀ"); DOUBLE_JONG.put("ㅂㅅ", "ㅄ");
    }

    public static String translate(String input) {
        StringBuilder jamo = new StringBuilder();
        for (char c : input.toCharArray()) {
            // 대문자 P, O를 인식하도록 수정 (indexOf 조건에 P와 O 추가)
            if ("QWERTYOP".indexOf(c) >= 0) jamo.append(KEY_MAP.getOrDefault(c, String.valueOf(c)));
            else jamo.append(KEY_MAP.getOrDefault(Character.toLowerCase(c), String.valueOf(c)));
        }
        return assemble(jamo.toString());
    }

    private static String assemble(String text) {
        StringBuilder result = new StringBuilder();
        int state = 0; // 0:초성대기, 1:초성입력, 2:중성입력, 3:종성입력
        int cho = -1, jung = -1, jong = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int cIdx = CHO.indexOf(c);
            int jIdx = JUNG.indexOf(c);
            int oIdx = JONG.indexOf(c) + 1;

            switch (state) {
                case 0:
                    if (cIdx >= 0) { cho = cIdx; state = 1; }
                    else result.append(c);
                    break;
                case 1:
                    if (jIdx >= 0) { jung = jIdx; state = 2; }
                    else if (cIdx >= 0) { result.append(CHO.charAt(cho)); cho = cIdx; }
                    else { result.append(CHO.charAt(cho)).append(c); state = 0; }
                    break;
                case 2:
                    int cJung = getCombinedJung(jung, jIdx);
                    if (jIdx >= 0 && cJung >= 0) { jung = cJung; }
                    else if (oIdx > 0) {
                        if (i + 1 < text.length() && JUNG.indexOf(text.charAt(i + 1)) >= 0) {
                            result.append(combine(cho, jung, 0));
                            cho = cIdx; state = 1;
                        } else { jong = oIdx; state = 3; }
                    } else {
                        result.append(combine(cho, jung, 0));
                        if (cIdx >= 0) { cho = cIdx; state = 1; } else { result.append(c); state = 0; }
                    }
                    break;
                case 3:
                    int cJong = getCombinedJong(jong, cIdx);
                    if (cJong > 0) { // 겹받침 가능 여부 체크
                        if (i + 1 < text.length() && JUNG.indexOf(text.charAt(i + 1)) >= 0) {
                            result.append(combine(cho, jung, jong));
                            cho = cIdx; state = 1; jong = 0;
                        } else { jong = cJong; } // 'ㅄ' 등으로 합침
                    } else if (jIdx >= 0) { // 연음 법칙 (있어 -> 이써)
                        result.append(combine(cho, jung, 0));
                        cho = CHO.indexOf(JONG.charAt(jong - 1));
                        jung = jIdx; state = 2; jong = 0;
                    } else {
                        result.append(combine(cho, jung, jong));
                        if (cIdx >= 0) { cho = cIdx; state = 1; jong = 0; }
                        else { result.append(c); state = 0; jong = 0; }
                    }
                    break;
            }
        }
        if (state == 1) result.append(CHO.charAt(cho));
        else if (state == 2) result.append(combine(cho, jung, 0));
        else if (state == 3) result.append(combine(cho, jung, jong));

        return result.toString();
    }

    private static int getCombinedJung(int f, int s) {
        if (f < 0 || s < 0) return -1;
        String combined = DOUBLE_JUNG.get("" + JUNG.charAt(f) + JUNG.charAt(s));
        return combined != null ? JUNG.indexOf(combined) : -1;
    }

    private static int getCombinedJong(int jIdx, int nextCIdx) {
        if (jIdx <= 0 || nextCIdx < 0) return -1;
        String combined = DOUBLE_JONG.get("" + JONG.charAt(jIdx - 1) + CHO.charAt(nextCIdx));
        return combined != null ? JONG.indexOf(combined) + 1 : -1;
    }

    private static char combine(int cho, int jung, int jong) {
        return (char) ((cho * 21 + jung) * 28 + jong + 0xAC00);
    }
}
package ir.hefze.quran;

import java.text.Normalizer;

/** Normalizes Arabic surah names and Persian keyboard input to the same spelling. */
public final class SurahSearch {
    private SurahSearch() {}

    public static String normalize(String value) {
        String result = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replace('ی', 'ي').replace('ى', 'ي')
                .replace('ك', 'ک').replace('ة', 'ه')
                .replace('ۀ', 'ه').replace('ٱ', 'ا').replace("ـ", "");
        for (int digit = 0; digit < 10; digit++) {
            result = result.replace((char) ('۰' + digit), (char) ('0' + digit))
                    .replace((char) ('٠' + digit), (char) ('0' + digit));
        }
        return result.trim();
    }
}

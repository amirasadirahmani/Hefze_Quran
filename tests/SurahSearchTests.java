import ir.hefze.quran.SurahSearch;

public final class SurahSearchTests {
    private static void matches(String keyboard, String title) {
        if (!SurahSearch.normalize(title).contains(SurahSearch.normalize(keyboard)))
            throw new AssertionError(keyboard + " should match " + title);
    }
    public static void main(String[] args) {
        matches("فاتحه", "سُورَةُ ٱلْفَاتِحَةِ");
        matches("بقره", "سُورَةُ البَقَرَةِ");
        matches("اعلی", "سُورَةُ الأَعۡلَىٰ");
        matches("ضحی", "سُورَةُ الضُّحَىٰ");
        matches("يس", "سُورَةُ يسٓ");
        if (!"36".equals(SurahSearch.normalize("۳۶")) || !"36".equals(SurahSearch.normalize("٣٦")))
            throw new AssertionError("Persian and Arabic surah numbers");
        System.out.println("PASS: surah search keyboard variants");
    }
}

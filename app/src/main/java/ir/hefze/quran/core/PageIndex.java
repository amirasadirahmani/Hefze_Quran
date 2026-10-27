package ir.hefze.quran.core;

/** Reference mushaf pages, indexed by the one-based global ayah number. */
public final class PageIndex {
    private final int[] pages;
    private final int[] first;
    public PageIndex(int[] ayahPages) {
        if (ayahPages.length == 0 || ayahPages[0] != 1) throw new IllegalArgumentException("First page");
        pages = ayahPages.clone();
        int previous = 1;
        for (int page : pages) {
            if (page < previous || page > previous + 1) throw new IllegalArgumentException("Page sequence");
            previous = page;
        }
        first = new int[previous + 1];
        for (int i = 0; i < pages.length; i++) if (first[pages[i]] == 0) first[pages[i]] = i + 1;
    }
    public int pageCount() { return first.length - 1; }
    public int restoreAyah(boolean hasReading, int id) { return hasReading ? Math.max(1, Math.min(pages.length, id)) : 1; }
    public int pageOf(int id) { return pages[restoreAyah(true, id) - 1]; }
    public int firstAyah(int page) { return first[Math.max(1, Math.min(pageCount(), page))]; }
    public int adjacent(int page, int delta) { return Math.max(1, Math.min(pageCount(), page + delta)); }
}

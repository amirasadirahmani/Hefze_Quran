package ir.hefze.quran.core;

public enum StudyMethod {
    AUDIO("صوتی", "گوش‌دادن و تکرار همراه با متن", 5,5,false),
    LINE("سطرمحور", "تمرین با جای ثابت سطرها",3,4,false),
    VISUAL("تصویری", "آزمون با متن پنهان و آشکارسازی دستی",3,5,true),
    VERBAL("کلام‌محور", "تکرار پیاپی و شمارش تمرین شخصی",5,5,true),
    MEANING("معنامحور", "فعلاً فقط تمرین متن؛ ترجمه در این نسخه نیست",3,3,false),
    TOPICAL("موضوعی", "فعلاً فقط یاسین؛ دسته‌بندی موضوعی در این نسخه نیست",3,3,false),
    SURAH("سوره‌محور", "تمرکز بر تکمیل سورهٔ یاسین",3,3,false);
    public final String title,description;
    public final int repeats,gap;
    public final boolean hidden;
    StudyMethod(String title,String description,int repeats,int gap,boolean hidden) {
        this.title=title;this.description=description;this.repeats=repeats;this.gap=gap;this.hidden=hidden;
    }
    public static StudyMethod safe(String name) { try { return valueOf(name); } catch(Exception e) { return AUDIO; } }
}

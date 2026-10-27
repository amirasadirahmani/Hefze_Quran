> نسخهٔ ۰.۸: [تغییرات پخش هنگام قفل و مقدمهٔ صوتی](docs/REVIEW_0.8.fa.md) · [ساخت APK](docs/BUILD_APK_ACTIONS.fa.md)

> نسخهٔ ۰.۷: اصلاح شروع مطالعه، تنظیمات کامل، تکرار و مکث قرائت و کنترل صفحه‌ها.
> [گزارش تغییرات](docs/REVIEW_0.7.fa.md) · [ساخت و دانلود APK با Actions](docs/BUILD_APK_ACTIONS.fa.md)

# حفظ قرآن برای اندروید

خواندن ۱۱۴ سوره با مرزهای ۶۰۴ صفحه، دریافت اختیاری صوت و تمرین صوتی با تعداد قرائت و مکث قابل تنظیم. محل مطالعه و تنظیمات روی گوشی ذخیره می‌شوند.

**نسخهٔ آزمایشی ۰٫۷:** رابط پسته‌ای، متن اعراب‌دار، جست‌وجوی فارسی و فیلتر جزء، انتخاب قاری و دانلود اختیاری صوت. MP3 همراه APK نیست. هایلایت کلمه‌ای همچنان به زمان‌بندی تأییدشده نیاز دارد. [گزارش تغییرات](docs/REVIEW_0.7.fa.md).

جزئیات تغییرات و شیوهٔ انتقال به گیت: [docs/PISTACHIO_UPDATE.fa.md](docs/PISTACHIO_UPDATE.fa.md).

## اجرا روی macOS با Apple Silicon

1. پوشهٔ پروژه را در Android Studio باز کنید. Gradle JDK را روی JDK 17 یا Embedded JDK سازگار تنظیم کنید.
2. در SDK Manager، Android SDK Platform 35 و Build Tools 35.0.0 را نصب کنید. Gradle Sync برای بار نخست به اینترنت نیاز دارد. متن و پیشرفت آفلاین‌اند؛ دریافت اختیاری صوت به اینترنت نیاز دارد.
3. برای شبیه‌ساز روی Apple Silicon، تصویر ARM64 با Android API 26 یا جدیدتر انتخاب کنید؛ یک گوشی واقعی هم مناسب است.
4. دکمهٔ Run را بزنید. برای ساخت APK از ترمینال داخل پروژه:

```bash
chmod +x gradlew tools/test_core.sh
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug :app:lintDebug
```

اگر JDK جداگانه نصب نیست، در ترمینال می‌توانید مسیر JDK همراه Android Studio را تنظیم کنید:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

خروجی: `app/build/outputs/apk/debug/app-debug.apk`. بستهٔ debug برای آزمایش است؛ کلید امضای انتشار در پروژه وجود ندارد.

## استفاده

- نصب تازه با فهرست و «شروع مطالعه» باز می‌شود؛ پس از مطالعه، «ادامهٔ مطالعه» محل واقعی را باز می‌کند.
- صفحه‌های ۱ تا ۶۰۴ بر اساس metadata منبع AlQuran Cloud هستند. اندازهٔ فونت قابل تغییر است؛ شکست خطوط الزاماً مشابه چاپ نیست. خطوط کامل justify هستند و خط آخر هر بخش طبیعی می‌ماند.
- تنظیمات صفحهٔ کامل دارد: فونت قرآن، اندازه، قاری، تعداد قرائت ۱ تا ۱۰ و مکث ۰ تا ۳۰ ثانیه. فقط روش حفظ صوتی فعال است؛ تم‌های کودک و ترجمه فعلاً غیرفعال‌اند.
- صوت هر سوره به‌صورت اختیاری دانلود و برای هر قاری جدا نگه‌داری می‌شود. فایل صوتی همراه APK نیست.
- راست «قبل» و چپ «بعد» است. دکمه‌های صفحه برچسب و کادر مجزا دارند. با لمس آیه هم می‌توان انتخاب را عوض کرد.
- در صوت آیه‌ای، آیهٔ در حال پخش برجسته و دنبال می‌شود. در فایل یکپارچهٔ سوره، دنبال‌کردن و تکرار آیه‌ای غیرفعال است.
- خروج از برنامه یا جداکردن هدفون پخش را مکث می‌کند. ادامه نیاز به لمس پخش دارد. پس از مرگ فرایند، آیه و تنظیمات بازگردانی می‌شوند؛ موقعیت زمانی صوت و شمارهٔ دور حفظ نمی‌شوند.

کدهای تمرین قدیمی یاسین برای سابقهٔ توسعه در پروژه باقی‌اند، اما در مسیر عادی خواننده ورودی ندارند. مستندات قدیمی مربوط به آن بخش، مشخصات قابلیت‌های خوانندهٔ فعلی محسوب نمی‌شوند.

## تحویل صوت و تصویر

راهنمای صوت و JSON: [docs/AUDIO_HANDOFF.fa.md](docs/AUDIO_HANDOFF.fa.md)

فایل‌های محلی زیر فقط برای تهیه و بررسی زمان‌بندی هستند و از بسته‌بندی APK کنار گذاشته می‌شوند. برای شروع، سه فایل `036-001.mp3`، `036-002.mp3` و `036-003.mp3` از یک قاری ثابت کافی‌اند؛ ساختار برای همهٔ ۸۳ آیه آماده است. پیش‌فرض تمرین را برای این نمونه روی آیات ۱ تا ۳ بگذارید. ابزار آفلاین `tools/align_words.html` با بازکردن در مرورگر امکان ثبت دستی مرز کلمات، بازشنوی و خروجی JSON دارای SHA-256 می‌دهد.

قاب ارسالی جدید در `res/drawable-nodpi/mushaf_pistachio.jpg` است؛ سربرگ، بدنه و پلیر متناسب با ارتفاع واقعی کنترل‌ها رسم می‌شوند. ابتدای سوره بسم‌الله و شمارهٔ صفحه، و در صفحه‌های بعد نام سوره و شمارهٔ صفحه در قاب بالا دیده می‌شوند. مشخصات و منابع در [docs/ASSETS.fa.md](docs/ASSETS.fa.md) آمده است.

## ساختار

```text
app/src/main/java/ir/hefze/quran/
  FullQuranActivity.java  خوانندهٔ اصلی، فهرست و کنترل پخش
  ReaderSettingsScreen.java صفحهٔ کامل تنظیمات
  QuranBook.java          متن کامل، صفحه‌ها و فهرست قاری‌ها
  BookDownloads.java      دریافت صوت برای خواننده
  BookPlayer.java         پخش صوت خواننده
  MainActivity.java       رابط تمرین قدیمی یاسین
  QuranPageView.java      رندر متن واقعی با چیدمان ثابت
  QuranRepository.java    آیات و اعتبارسنجی زمان‌بندی
  AudioDownloads.java    دریافت اختیاری HTTPS و ذخیرهٔ فایل کامل
  SurahCatalog.java       فهرست محلی سوره‌ها و فیلتر جزء
  MushafFrameDrawable.java قاب متناسب با سربرگ و پلیر
  AudioController.java    اتصال MediaPlayer به صف و مدیریت چرخهٔ عمر
  StudyStore.java         SQLite و گزارش جلسات
  ReminderReceiver.java   یادآوری اختیاری محلی
  ProgressChartView.java  نمودار هفت روز
  core/                  ماشین حالت مستقل و قابل تست
app/src/main/assets/
  content/yasin.json      متن، کلمات و ده صفحهٔ ثابت
  audio/036/             جای ۸۳ فایل MP3
  timings/036/           ۸۳ قالب زمان‌بندی بدون زمان تخمینی
  fonts/                 Scheherazade New و مجوز OFL؛ Amiri نسخهٔ قبلی
```

Java 17 و Android APIهای داخلی انتخاب شده‌اند؛ وابستگی اجرایی ثالثی لازم نیست. `minSdk=26` و `target/compileSdk=35`؛ AGP 8.9.2 با Gradle 8.11.1 تثبیت شده است. این انتخاب ادعای استفاده از آخرین نسخه نیست. سازگاری ساخت طبق [مستند رسمی AGP 8.9](https://developer.android.com/build/releases/agp-8-9-0-release-notes) انتخاب شده است. MediaPlayer طبق [مرجع رسمی](https://developer.android.com/reference/android/media/MediaPlayer) پشت یک adapter مستقل قرار دارد.

## بررسی‌ها

```bash
./tools/test_core.sh
python3 tools/validate_assets.py
python3 -m unittest discover -s tests -v
java tools/ParseJava.java app/src/main/java
```

برای بررسی بستهٔ مرجع شامل صوت و هایلایت کامل (مجزا از APK دارای دانلود اختیاری):

```bash
python3 tools/validate_assets.py --release
./gradlew :app:assembleDebug :app:lintDebug
```

حالت `--release` برای بستهٔ مرجع صوت و زمان‌بندی است؛ وضعیت دانلودهای گوشی را بررسی نمی‌کند. در وضعیت فعلی عمداً خطا می‌دهد: ۸۳ صوت و ۸۳ زمان‌بندی تأییدشده موجود نیست. `test_assets.py` به وضعیت پایهٔ بدون صوت وابسته نیست؛ تست‌ها با fixture بدون صوت اجرا می‌شوند. زمان‌بندی تنها با `reviewed=true`، تطابق کلمات، مرزهای معتبر و SHA-256 همان صوت فعال می‌شود. JSONهای فعلی `null` دارند و زمان‌بندی ساختگی نیستند.

آزمون‌های گوشی و موارد پذیرش: [docs/ACCEPTANCE.fa.md](docs/ACCEPTANCE.fa.md). ریسک‌ها و اصلاحات پروپوزال: [docs/IMPLEMENTATION.fa.md](docs/IMPLEMENTATION.fa.md).

سورس این نسخه در ریپازیتوری GitHub قرار دارد. برای مشاهدهٔ APK آزمایشی به تب Actions و اجرای Android development build بروید.

گزارش دقیق بررسی‌های انجام‌شده: [docs/TEST_REPORT.md](docs/TEST_REPORT.md). گردش‌کار GitHub Actions برای ساخت APK آزمایشی در `.github/workflows/android.yml` آماده است و پس از push یا اجرای دستی، APK را به‌صورت Artifact ارائه می‌کند.


## نسخهٔ ۰٫۵ خوانندهٔ کامل قرآن

خوانندهٔ جدید ۱۱۴ سوره را با مرز ۶۰۴ صفحه و متن اعراب‌دار نمایش می‌دهد. سطرهای کامل با `JUSTIFICATION_MODE_INTER_WORD` در هر دو سمت تراز می‌شوند؛ خط پایانی هر بخش سوره کوتاه می‌ماند. تنظیمات قاری، دانلود سوره و پخش آیه‌ای در خواننده قرار دارند. نسخه‌های صوتی یکپارچهٔ سوره به‌دلیل نداشتن زمان‌بندی آیه، همراهی خودکار متن را ارائه نمی‌کنند. راهنمای نصب آزمایشی روی گوشی: [ADB_TEST.fa.md](docs/ADB_TEST.fa.md).

برای دریافت کد کامل، از دکمهٔ **Code → Download ZIP** در صفحهٔ مخزن استفاده کنید؛ این همان درخت کد commit شده است.

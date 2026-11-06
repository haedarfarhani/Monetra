## بازطراحی کامل صفحه گزارش‌ها (Analytics) — Monetra

### Context (زمینه)
صفحه فعلی گزارش‌ها (`feature/analytics/AnalyticsScreen.kt`) فقط ۴ بخش ساده دارد: چیپ‌های بازه، کارت درآمد/هزینه، نمودار میله‌ای ماهانه دستی، و لیست دسته‌ها. ViewModel مقادیر مفیدی (جریان روزانه، میانگین‌ها، بزرگ‌ترین هزینه، تعداد تراکنش) را محاسبه می‌کند اما نمایش داده نمی‌شوند. یک **باگ تقویم شمسی** هم وجود دارد: `calculateMonthlyTrends` از `java.util.Calendar` استفاده می‌کند و با تقویم شمسی مرز ماه‌ها/برچسب‌ها اشتباه می‌شود. پروژه هیچ کتابخانه نمودار ندارد و همه نمودارها با Canvas دستی در سبک «Liquid Glass» کشیده می‌شوند — همین رویکرد را ادامه می‌دهیم (بدون وابستگی جدید، هماهنگ با زبان طراحی اپ).

### نتیجه نهایی برای کاربر
یک صفحه گزارش حرفه‌ای با: چیپ‌های بازه (این ماه / ۳ ماه / امسال / همه)، کارت خلاصه با **مقایسه با دوره قبل** (٪ بالا/پایین)، نمودارهای **خطی زیبا با انیمیشن و Tooltip لمسی** (روزانه/ماهانه/سالانه)، شبکه میانگین‌ها (میانگین هزینه روزانه، میانگین درآمد/هزینه ماهانه، نرخ پس‌انداز، تعداد تراکنش)، **نمودار دونات** دسته‌ها + لیست، و کارت‌های بینش (بزرگ‌ترین هزینه، پرخرج‌ترین دسته، پرخرج‌ترین روز، پیش‌بینی هزینه تا پایان ماه).

---

### ۱. کیت نمودار قابل‌استفاده مجدد (فایل‌های جدید در `core/designsystem/charts/`)
- **`MonetraLineChart.kt`** — نمودار خطی منحنی (Bezier) با گرادیانت زیر منحنی، پشتیبانی ۲ سری (درآمد/هزینه)، خط‌چین میانگین، انیمیشن رسم (progress reveal)، و **تعامل لمسی**: خط راهنما + نقطه + Tooltip (با `rememberTextMeasurer`) با درگ روی نمودار. برچسب‌های محور X با نمونه‌برداری (sampling) برای جلوگیری از شلوغی.
- **`MonetraBarChart.kt`** — میله‌ای گروهی مدرن با گرادیانت، Tooltip لمسی و انیمیشن رشد میله‌ها (جایگزین `MonthlyCanvasChart` فعلی).
- **`MonetraDonutChart.kt`** — دونات انیمیشنی دسته‌ها با فاصله بین اسلایس‌ها، انتخاب لمسی اسلایس، `centerContent` برای نمایش مبلغ کل در مرکز.

### ۲. منطق محاسبات — `feature/analytics/AnalyticsCalculator.kt` (جدید، Pure Kotlin)
تمام تجمیع‌ها از ViewModel به یک کلاس خالص (بدون Android dep، با ورودی `now: Long` برای تست‌پذیری) منتقل/گسترش می‌یابند:
- `buildDailyPoints` — باکت روزانه بر اساس `LocalDate` (بدون خطای DST)، برچسب با `PersianDigits`؛ برای بازه‌های > ۱۵۰ نقطه، تجمیع هفتگی خودکار.
- `buildMonthlyPoints` — **رفع باگ شمسی**: گام‌زدن ماه‌ها فقط با `AppDateFormatter.monthRange` (مثل الگوی درست `DashboardViewModel.buildMonthlyTrend`)، برچسب با `PersianDate.monthName()` برای fa.
- `buildYearlyPoints` — تجمیع سالانه ۵ سال اخیر بر اساس تقویم انتخابی (سال شمسی با تبدیل `JalaliCalendar`).
- `computeComparison` — درآمد/هزینه دوره قبل هم‌طول برای نشان ٪ تغییر.
- `computeAverages` (میانگین روزانه/ماهانه + نرخ پس‌انداز = net/income)، `computeHighestSpendDay`، `computeProjection` (میانگین روزانه × تعداد روز ماه، فقط برای THIS_MONTH).
- افزودن `yearRange(timestamp, calendar)` به `AppDateFormatter` (شروع/پایان سال شمسی از طریق `JalaliCalendar`).

### ۳. گسترش State و ViewModel
- `AnalyticsUiState.kt`: enum `AnalyticsGranularity` (DAILY/MONTHLY/YEARLY)، `YearlyTrendData`، فیلدهای جدید: `incomeChangePct/expenseChangePct`، `savingsRate`، `highestSpendDay`، `projectedMonthExpense`، `topCategory`، افزودن `colorHex` به `CategorySpendData` (رنگ واقعی دسته از DB برای دونات).
- `AnalyticsViewModel.kt`: `selectGranularity`، اتصال به `AnalyticsCalculator`، بازه «همه» (ALL) به `AnalyticsRange` اضافه می‌شود. پاک‌سازی محاسبات تکراری.

### ۴. بازطراحی UI — `AnalyticsScreen.kt` (بازنویسی کامل)
ساختار LazyColumn از بالا به پایین:
1. چیپ‌های بازه (۴ چیپ با `GlassFilterChip` موجود)
2. **کارت خلاصه دوره**: مبلغ خالص بزرگ + درآمد/هزینه + نوار نسبت + ردیف مقایسه با دوره قبل (فلش ↑↓ رنگی و ٪)
3. **کارت روند**: تب‌های بخش‌بندی (روزانه/ماهانه/سالانه) + Legend + نمودار خطی (روزانه و سالانه) / میله‌ای (ماهانه) با Tooltip
4. **شبکه میانگین‌ها** (گرید ۲×۲): میانگین هزینه روزانه، میانگین درآمد ماهانه، میانگین هزینه ماهانه، نرخ پس‌انداز + ردیف تعداد تراکنش‌ها
5. **دسته‌ها**: دونات با رنگ واقعی دسته‌ها + مبلغ کل در مرکز، سپس ردیف‌های دسته (بهبود `CategoryProgressRow` با رنگ دسته)
6. **بینش‌ها**: کارت‌های کوچک — بزرگ‌ترین هزینه، پرخرج‌ترین دسته، پرخرج‌ترین روز، پیش‌بینی تا پایان ماه (شرطی)
7. EmptyState موجود برای داده خالی، LoadingIndicator موجود

### ۵. Strings (۳ فایل)
افزودن ~۲۵ رشته جدید به `res/values/strings.xml`، `values-en/`، `values-fa/` (مثلاً `analytics_range_all`، `analytics_tab_daily`، `analytics_savings_rate`، `analytics_avg_daily_expense`، `analytics_vs_previous`، `analytics_insights_title`، `analytics_biggest_expense`، `analytics_projection`، ...).

### ۶. تست — `AnalyticsCalculatorTest.kt` (جدید)
تست تجمیع روزانه/ماهانه، **صحت مرز ماه‌های شمسی** (مثل `JalaliCalendarTest` موجود)، میانگین‌ها، نرخ پس‌انداز، مقایسه دوره قبل، پیش‌بینی.

### فایل‌های کلیدی
| عمل | مسیر |
|---|---|
| جدید | `core/designsystem/charts/MonetraLineChart.kt`، `MonetraBarChart.kt`، `MonetraDonutChart.kt` |
| جدید | `feature/analytics/AnalyticsCalculator.kt`، `app/src/test/.../AnalyticsCalculatorTest.kt` |
| تغییر | `feature/analytics/AnalyticsScreen.kt`، `AnalyticsViewModel.kt`، `AnalyticsUiState.kt` |
| تغییر | `core/calendar/AppDateFormatter.kt` (+`yearRange`) |
| تغییر | هر ۳ فایل strings |

### Verification
1. `./gradlew.bat :app:compileDebugKotlin` — بیلد موفق
2. `./gradlew.bat :app:testDebugUnitTest --tests "ir.heydar.monetra.feature.analytics.AnalyticsCalculatorTest" --tests "ir.heydar.monetra.core.calendar.*"` — تست‌های جدید و موجود سبز
3. اجرای کامل `testDebugUnitTest` برای اطمینان از نشکستن بقیه
4. بازبینی بصری نهایی با کاربر (RTL فارسی + دارک‌مود: چون همه رنگ‌ها از `monetraColors`/`glassColors` می‌آیند، هر دو تم خودکار رعایت می‌شود)

# کارت‌ساز هوشمند نگاه جوان — 1.1.0 Hybrid AI Studio

نسخه 1.1 از موتور ترکیب‌بندی، صفحه‌بندی، کراپ چهره، انتخاب هویت بصری و تحلیل Hybrid استفاده می‌کند.

## Smart
- Smart Composition برای تیتر، متن، فاصله‌ها، فضای عکس و Safe Area
- Smart Pagination با حفظ متن کامل
- Smart Image با ML Kit و Smart Crop / Fit Blur
- Exact Preview از همان Bitmap Renderer خروجی
- Hybrid Analysis با Backend آنلاین و fallback محلی
- Online AI Image با fallback گرافیکی محلی
- ذخیره پیش‌نویس متن و URI عکس انتخابی

## خروجی‌ها
- Story 1080×1920
- Portrait 1080×1350
- Square 1080×1080
- Landscape 1920×1080
- PNG و Share مستقیم

## Backend
پوشه server شامل FastAPI است. مدل پیش‌فرض تحلیل gpt-6-luna و مدل تصویر gpt-image-2.5-flare است.

برای فعال شدن AI آنلاین:
- OPENAI_API_KEY روی Backend
- AI_BASE_URL هنگام Build اپ

در نبود Backend یا API، برنامه بدون Crash با موتور محلی ادامه می‌دهد.

## CI
- تست Backend
- Unit Test اندروید
- Android lint
- Build APK

## انتشار
APK تستی مستقیماً قابل نصب است. برای آپدیت پایدار فروشگاهی باید keystore خصوصی دائمی به GitHub Secrets اضافه شود.

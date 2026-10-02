# Project Status — 1.1.0

## Done
- Smart Composition
- Smart Pagination با متن کامل
- Face-aware crop
- سه هویت بصری مستقل
- Exact Preview با همان Renderer خروجی
- چهار نسبت خروجی
- Backend Hybrid AI
- تولید تصویر AI آنلاین با fallback گرافیکی
- ذخیره پیش‌نویس و URI عکس
- نمایش وضعیت AI آنلاین/محلی
- Backend test + Android unit test + lint
- حذف permission غیرضروری READ_MEDIA_IMAGES
- یکسان‌سازی منطق «هم‌اکنون»

## External dependency
AI آنلاین زمانی فعال می‌شود که Backend HTTPS، OPENAI_API_KEY و AI_BASE_URL تنظیم شده باشند.

Railway فعلی حساب به سقف provision پلن رایگان رسیده و ایجاد سرویس جدید را رد می‌کند؛ بنابراین کد Backend آماده است اما استقرار مستقل جدید نیازمند آزاد شدن یک resource یا میزبان دیگری است.

## Signing
برای انتشار نهایی با امکان Update مطمئن، keystore خصوصی دائمی باید در Secrets قرار بگیرد. CI فعلاً APK تستی می‌سازد.


## Release
- 1.1.0 promoted to main for final CI and APK publication.

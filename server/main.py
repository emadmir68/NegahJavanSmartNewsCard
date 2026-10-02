import json
import os
import re
import time
from collections import defaultdict, deque

from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from starlette.responses import JSONResponse

app = FastAPI(title="Negah Javan Smart News API", version="1.1.0")

origins = [x.strip() for x in os.getenv("CORS_ORIGINS", "").split(",") if x.strip()]
if origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=origins,
        allow_methods=["GET", "POST"],
        allow_headers=["Content-Type", "Accept"],
    )

WINDOW_SECONDS = 600
MAX_REQUESTS = 80
_requests = defaultdict(deque)


@app.middleware("http")
async def rate_limit(request: Request, call_next):
    if request.url.path in {"/analyze", "/generate-image"}:
        host = request.client.host if request.client else "unknown"
        now = time.time()
        queue = _requests[host]
        while queue and now - queue[0] > WINDOW_SECONDS:
            queue.popleft()
        if len(queue) >= MAX_REQUESTS:
            return JSONResponse({"detail": "rate limit exceeded"}, status_code=429)
        queue.append(now)
    return await call_next(request)


class AnalyzeRequest(BaseModel):
    text: str = Field(min_length=3, max_length=30000)
    client_version: str | None = None


class AnalyzeResponse(BaseModel):
    title: str
    lead: str
    category: str
    badge: str
    length: str
    template: str
    premium_template: str
    accent_word: str
    hashtags: list[str]
    source: str
    confidence: float


class ImageRequest(BaseModel):
    title: str = Field(min_length=3, max_length=220)
    text: str = Field(default="", max_length=12000)
    category: str = Field(default="GENERAL", max_length=40)


class ImageResponse(BaseModel):
    image_base64: str
    source: str
    model: str


@app.get("/health")
def health():
    return {
        "ok": True,
        "ai_enabled": bool(os.getenv("OPENAI_API_KEY")),
        "version": "1.1.0",
        "model": os.getenv("OPENAI_MODEL", "gpt-6-luna"),
        "image_model": os.getenv("OPENAI_IMAGE_MODEL", "gpt-image-2.5-flare"),
        "mode": "hybrid-ai-with-on-device-fallback",
    }


@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest):
    key = os.getenv("OPENAI_API_KEY")
    if not key:
        return fallback(req.text, source="server-fallback")

    try:
        from openai import OpenAI
        client = OpenAI(api_key=key)
        prompt = f"""
نقش تو دبیر و مدیر هنری یک پایگاه خبری فارسی است.
فقط درباره متن داده‌شده تصمیم بگیر و هیچ واقعیت تازه‌ای اضافه نکن.

فقط JSON معتبر با کلیدهای زیر برگردان:
title, lead, category, badge, length, template, premium_template, accent_word, hashtags

مقادیر مجاز:
category: BREAKING|JUDICIAL|POLITICS|SOCIAL|LOCAL|ECONOMY|SPORTS|CULTURE|TECHNOLOGY|GENERAL
length: SHORT|MEDIUM|LONG
template: SIGNATURE|BREAKING_PLUS|EDITORIAL
premium_template: LUXURY_MINIMAL|PHOTO_PREMIUM|EDITORIAL_GLASS

قواعد:
- اگر خط اول تیتر مناسب است آن را حفظ کن.
- لید حداکثر ۱۸۰ نویسه و فقط بر اساس همین خبر باشد.
- «فوری» فقط با فوریت زمانی روشن؛ صرف «هم‌اکنون» کافی نیست.
- متن اصلی نباید حذف یا بازنویسی اجباری شود؛ اپ متن کامل را صفحه‌بندی می‌کند.
- خبر قضایی/سیاسی/اقتصادی سنگین: LUXURY_MINIMAL
- خبر تصویری/کوتاه: PHOTO_PREMIUM
- خبر فوری/اجتماعی/هشدار: EDITORIAL_GLASS
- hashtags آرایه‌ای حداکثر پنج‌تایی و بدون # باشد.

خبر:
{req.text}
"""
        response = client.responses.create(
            model=os.getenv("OPENAI_MODEL", "gpt-6-luna"),
            input=[{"role": "user", "content": prompt}],
        )
        raw = response.output_text.strip()
        fence = chr(96) * 3
        if raw.startswith(fence):
            raw = raw.split("\n", 1)[1] if "\n" in raw else raw[3:]
            if raw.endswith(fence):
                raw = raw[:-3]
        data = json.loads(raw.strip())
        local = fallback(req.text, source="online-ai")

        return AnalyzeResponse(
            title=clean_text(data.get("title")) or local.title,
            lead=clean_text(data.get("lead"))[:180] or local.lead,
            category=validate_choice(data.get("category"), CATEGORIES, local.category),
            badge=clean_text(data.get("badge"))[:48] or local.badge,
            length=validate_choice(data.get("length"), LENGTHS, local.length),
            template=validate_choice(data.get("template"), TEMPLATES, local.template),
            premium_template=validate_choice(
                data.get("premium_template"),
                PREMIUM_TEMPLATES,
                local.premium_template,
            ),
            accent_word=clean_text(data.get("accent_word"))[:40] or local.accent_word,
            hashtags=normalize_hashtags(data.get("hashtags")) or local.hashtags,
            source="online-ai",
            confidence=0.90,
        )
    except Exception:
        return fallback(req.text, source="server-fallback")


@app.post("/generate-image", response_model=ImageResponse)
def generate_image(req: ImageRequest):
    key = os.getenv("OPENAI_API_KEY")
    if not key:
        raise HTTPException(status_code=503, detail="image AI is not configured")

    try:
        from openai import OpenAI
        client = OpenAI(api_key=key)
        model = os.getenv("OPENAI_IMAGE_MODEL", "gpt-image-2.5-flare")
        prompt = f"""
تصویر خبری حرفه‌ای، واقع‌گرایانه و بدون نوشته، لوگو یا واترمارک بساز.
فقط از موضوع همین خبر استفاده کن. چهره مشخصِ فردی را بدون تصویر مرجع جعل نکن.
ترکیب‌بندی باید برای کارت خبری مناسب باشد و فضای تنفس کافی داشته باشد.

دسته: {req.category}
تیتر: {req.title}
متن مرجع: {req.text[:3500]}
"""
        response = client.images.generate(
            model=model,
            prompt=prompt,
            size="1536x1024",
            quality="medium",
            output_format="jpeg",
        )
        encoded = response.data[0].b64_json
        if not encoded:
            raise RuntimeError("empty image")
        return ImageResponse(
            image_base64=encoded,
            source="online-ai-image",
            model=model,
        )
    except HTTPException:
        raise
    except Exception as exc:
        raise HTTPException(status_code=502, detail="image generation failed") from exc


CATEGORIES = {
    "BREAKING", "JUDICIAL", "POLITICS", "SOCIAL", "LOCAL",
    "ECONOMY", "SPORTS", "CULTURE", "TECHNOLOGY", "GENERAL",
}
LENGTHS = {"SHORT", "MEDIUM", "LONG"}
TEMPLATES = {"SIGNATURE", "BREAKING_PLUS", "EDITORIAL"}
PREMIUM_TEMPLATES = {"LUXURY_MINIMAL", "PHOTO_PREMIUM", "EDITORIAL_GLASS"}


def clean_text(value) -> str:
    if value is None:
        return ""
    return re.sub(r"\s+", " ", str(value)).strip()


def validate_choice(value, allowed, fallback):
    candidate = clean_text(value).upper()
    return candidate if candidate in allowed else fallback


def normalize_hashtags(value) -> list[str]:
    if not isinstance(value, list):
        return []
    output = []
    for item in value:
        tag = clean_text(item).lstrip("#").replace(" ", "_")
        if tag and tag not in output:
            output.append(tag[:48])
    return output[:5]


def fallback(text: str, source: str = "server-fallback") -> AnalyzeResponse:
    title, body = parse_source(text)
    category = detect_category(text)
    length = detect_length(text)
    return AnalyzeResponse(
        title=title,
        lead=first_sentence(body, 180),
        category=category,
        badge=detect_badge(text, category),
        length=length,
        template=choose_template(category, length),
        premium_template=choose_premium_template(category, length, bool(body)),
        accent_word=pick_accent(title),
        hashtags=["نگاه_جوان", category_to_fa(category)],
        source=source,
        confidence=0.68,
    )


def parse_source(text: str) -> tuple[str, str]:
    normalized = "\n\n".join(
        line.strip() for line in text.replace("\r", "").splitlines() if line.strip()
    )
    lines = [clean_decorative(x) for x in normalized.splitlines() if x.strip()]
    if not lines:
        return "خبر جدید", ""

    first = lines[0]
    looks_title = (
        len(lines) >= 2
        and 12 <= len(first) <= 180
        and not first.endswith((".", "؛"))
    )
    if looks_title:
        return first, "\n\n".join(lines[1:]).strip()

    title = first_sentence(normalized, 118) or clean_text(normalized)[:118]
    if len(title) >= 118 and not title.endswith(("؟", "!", ".", "؛")):
        title = title.rstrip(" ،,:؛") + "…"

    body = "\n\n".join(lines).strip()
    prefix = title.rstrip("…")
    if prefix and body.startswith(prefix):
        body = body[len(prefix):].lstrip(" \n،,:؛.!؟")
    return title or "خبر جدید", body


def clean_decorative(value: str) -> str:
    value = str(value)
    for mark in ("⚖️", "⚖", "🔷️", "🔷", "🔹", "🔸", "▪️", "▪", "▫️", "▫"):
        value = value.replace(mark, "")
    value = re.sub(r"^[-–—•]+\s*", "", value)
    return clean_text(value)


def first_sentence(text: str, limit: int) -> str:
    flat = clean_text(text.replace("\n", " "))
    if not flat:
        return ""
    parts = re.split(r"(?<=[.!؟])\s+", flat)
    selected = next((x for x in parts if len(x.strip()) >= 20), parts[0] if parts else flat)
    if len(selected) <= limit:
        return selected.strip()
    cut = selected[:limit]
    space = cut.rfind(" ")
    if space > int(limit * 0.65):
        cut = cut[:space]
    return cut.rstrip(" ،,:؛") + "…"


def detect_length(text: str) -> str:
    chars = len(" ".join(text.split()))
    sentences = len([x for x in re.split(r"[.!؟]+", text) if len(x.strip()) > 20])
    if chars <= 360 and sentences <= 3:
        return "SHORT"
    if chars <= 1050 and sentences <= 7:
        return "MEDIUM"
    return "LONG"


def detect_category(text: str) -> str:
    t = text.lower()
    if any(x in t for x in ["خبر فوری", "فوری:", "فوری؛", "هشدار فوری", "لحظاتی پیش"]):
        return "BREAKING"
    if any(x in t for x in ["دادگستری", "دادگاه", "دادستان", "قضایی", "حقوق عامه"]):
        return "JUDICIAL"
    if any(x in t for x in ["انتخابات", "مجلس", "دولت", "وزیر", "سیاسی"]):
        return "POLITICS"
    if any(x in t for x in ["فوتبال", "ورزش", "تیم", "مسابقه"]):
        return "SPORTS"
    if any(x in t for x in ["اقتصاد", "قیمت", "بازار", "بورس", "دلار"]):
        return "ECONOMY"
    if any(x in t for x in ["فناوری", "هوش مصنوعی", "اینترنت", "اپلیکیشن"]):
        return "TECHNOLOGY"
    if any(x in t for x in ["فرهنگ", "هنر", "سینما", "کتاب"]):
        return "CULTURE"
    if any(x in t for x in ["ساردوئیه", "جیرفت", "کهنوج", "عنبرآباد", "روستا", "شهرستان"]):
        return "LOCAL"
    if any(x in t for x in ["شهرداری", "شهروند", "اجتماعی", "آموزش", "مدرسه", "هواشناسی", "بارش", "جاده", "محور"]):
        return "SOCIAL"
    return "GENERAL"


def detect_badge(text: str, category: str) -> str:
    t = text.lower()
    weather = ["باران", "بارش", "رعدوبرق", "رعد و برق", "طوفان", "گرد و غبار", "هواشناسی", "برف"]
    if "گزارش تصویری" in t:
        return "گزارش تصویری"
    if "انتصاب" in t or "منصوب شد" in t or "معارفه" in t:
        return "انتصاب"
    if "اطلاعیه" in t:
        return "اطلاعیه"
    if "هشدار" in t and any(x in t for x in weather):
        return "هشدار جوی"
    if "مطالبه" in t and any(x in t for x in ["مردم", "شهرستان", "روستا"]):
        return "مطالبه مردم"
    return category_to_fa(category)


def category_to_fa(category: str) -> str:
    return {
        "BREAKING": "فوری",
        "JUDICIAL": "قضایی",
        "POLITICS": "سیاسی",
        "SOCIAL": "اجتماعی",
        "LOCAL": "محلی",
        "ECONOMY": "اقتصادی",
        "SPORTS": "ورزشی",
        "CULTURE": "فرهنگی",
        "TECHNOLOGY": "فناوری",
    }.get(category, "خبر")


def choose_template(category: str, length: str) -> str:
    if category == "BREAKING":
        return "BREAKING_PLUS"
    if length == "LONG" or category in {"JUDICIAL", "POLITICS", "ECONOMY"}:
        return "EDITORIAL"
    return "SIGNATURE"


def choose_premium_template(category: str, length: str, has_body: bool) -> str:
    if category in {"BREAKING", "SOCIAL"}:
        return "EDITORIAL_GLASS"
    if length == "LONG" or category in {"JUDICIAL", "POLITICS", "ECONOMY"}:
        return "LUXURY_MINIMAL"
    return "PHOTO_PREMIUM" if has_body else "LUXURY_MINIMAL"


def pick_accent(title: str) -> str:
    words = [re.sub(r"[،,:؛.!؟]", "", x) for x in title.split()]
    words = [x for x in words if len(x) >= 5]
    return max(words, key=len) if words else ""

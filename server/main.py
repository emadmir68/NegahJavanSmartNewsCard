import json
import os
import re
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

app = FastAPI(title="Negah Javan Smart News API", version="0.3.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class AnalyzeRequest(BaseModel):
    text: str = Field(min_length=3, max_length=20000)

class SmartSlide(BaseModel):
    title: str
    body: str
    badge: str = ""

class AnalyzeResponse(BaseModel):
    title: str
    lead: str
    category: str
    badge: str
    length: str
    template: str
    slides: list[SmartSlide]
    accent_word: str = ""
    hashtags: list[str] = []
    decision_note: str = ""

@app.get("/health")
def health():
    return {"ok": True, "ai": bool(os.getenv("OPENAI_API_KEY")), "version": "0.3.0"}

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest):
    key = os.getenv("OPENAI_API_KEY")
    if not key:
        return fallback(req.text)

    try:
        from openai import OpenAI
        client = OpenAI(api_key=key)
        prompt = f"""
تو سردبیر هوشمند پایگاه خبری «نگاه جوان» هستی.
کاربر فقط متن خبر را می‌دهد و تو باید تمام تصمیم‌های تحریریه‌ای لازم برای ساخت کارت را بگیری.

فقط JSON معتبر برگردان:
{{
  "title":"تیتر کوتاه و دقیق کارت",
  "lead":"لید کوتاه",
  "category":"BREAKING|JUDICIAL|POLITICS|SOCIAL|LOCAL|ECONOMY|SPORTS|CULTURE|TECHNOLOGY|GENERAL",
  "badge":"یکی از برچسب‌های مناسب مثل فوری، قضایی، مطالبه مردم، گزارش، اطلاعیه، ویژه",
  "length":"SHORT|MEDIUM|LONG",
  "template":"SIGNATURE|BREAKING_PLUS|EDITORIAL",
  "slides":[
    {{"title":"...","body":"...","badge":"..."}}
  ],
  "accent_word":"...",
  "hashtags":["...","..."],
  "decision_note":"یک جمله کوتاه درباره تصمیم خودکار"
}}

قواعد:
- هیچ واقعیت تازه‌ای اضافه نکن.
- «فوری» را فقط اگر خبر واقعاً ماهیت فوری دارد انتخاب کن؛ صرف اهمیت خبر کافی نیست.
- خبر کوتاه: 1 اسلاید.
- خبر متوسط: 1 اسلاید کامل.
- خبر بلند: دقیقاً 4 اسلاید، بدون بریدن جمله در وسط.
- اسلاید اول کاور و خلاصه باشد.
- اسلایدهای 2 و 3 نکات اصلی و جزئیات مهم را پوشش دهند.
- اسلاید 4 جمع‌بندی باشد.
- title حداکثر 110 کاراکتر و lead حداکثر 200 کاراکتر.
- برای خبر رسمی/قضایی یا متن بلند معمولاً EDITORIAL بهتر است.
- BREAKING_PLUS فقط برای خبر واقعاً فوری.
- خروجی باید برای استوری و کاروسل شبکه اجتماعی خوانا باشد.

خبر:
{req.text}
"""
        response = client.responses.create(
            model=os.getenv("OPENAI_MODEL", "gpt-5.6-luna"),
            input=[{"role": "user", "content": prompt}],
        )
        raw = response.output_text.strip()
        raw = re.sub(r"^\x60\x60\x60(?:json)?\s*|\s*\x60\x60\x60$", "", raw)
        data = json.loads(raw)
        return AnalyzeResponse(**data)
    except Exception:
        return fallback(req.text)

def fallback(text: str) -> AnalyzeResponse:
    clean = " ".join(text.split())
    first_line = next((x.strip() for x in text.splitlines() if x.strip()), "")
    title = first_line if 12 <= len(first_line) <= 110 else shorten(clean, 108)
    category = detect_category(clean)
    length = detect_length(clean)
    badge = detect_badge(clean, category)
    template = choose_template(category, length)

    sentences = [x.strip() for x in re.split(r"(?<=[.!؟])\s+|(?=🔷)|(?=▪)|(?=•)", clean) if len(x.strip()) > 20]
    lead = shorten(sentences[1] if len(sentences) > 1 else clean, 190)

    if length != "LONG":
        body = shorten(" ".join(sentences[1:4]) or lead, 330)
        slides = [SmartSlide(title=title, body=body, badge=badge)]
    else:
        details = [s for s in sentences if title[:40] not in s]
        third = max(1, (len(details) + 2) // 3)
        parts = [
            " ".join(details[:third]),
            " ".join(details[third:third*2]),
            " ".join(details[third*2:]),
        ]
        headings = ["نکات اصلی", "ادامه خبر", "جمع‌بندی"]
        slides = [SmartSlide(title=title, body=lead, badge=badge)]
        for i, part in enumerate(parts):
            slides.append(SmartSlide(
                title=headings[i],
                body=shorten(part or lead, 430),
                badge="جمع‌بندی" if i == 2 else "ادامه"
            ))
        slides = slides[:4]

    words = [w.strip("،,:؛.!؟") for w in title.split() if len(w.strip("،,:؛.!؟")) >= 5]
    accent = max(words, key=len) if words else ""

    return AnalyzeResponse(
        title=title or "خبر جدید",
        lead=lead,
        category=category,
        badge=badge,
        length=length,
        template=template,
        slides=slides,
        accent_word=accent,
        hashtags=["نگاه_جوان", category.lower()],
        decision_note=decision_note(length, len(slides), template, category),
    )

def shorten(text: str, max_len: int) -> str:
    text = " ".join(text.split())
    if len(text) <= max_len:
        return text
    part = text[:max_len]
    cut = part.rfind(" ")
    if cut > int(max_len * 0.65):
        part = part[:cut]
    return part.rstrip(" ،,:؛") + "…"

def detect_length(text: str) -> str:
    n = len(text)
    if n < 420:
        return "SHORT"
    if n < 1050:
        return "MEDIUM"
    return "LONG"

def detect_category(text: str) -> str:
    if any(x in text for x in ["فوری", "لحظاتی", "هشدار فوری"]): return "BREAKING"
    if any(x in text for x in ["دادگستری", "دادگاه", "دادستان", "قضایی", "حقوق عامه"]): return "JUDICIAL"
    if any(x in text for x in ["انتخابات", "مجلس", "دولت", "وزیر", "سیاسی"]): return "POLITICS"
    if any(x in text for x in ["فوتبال", "ورزش", "تیم", "مسابقه"]): return "SPORTS"
    if any(x in text for x in ["اقتصاد", "قیمت", "بازار", "بورس", "دلار"]): return "ECONOMY"
    if any(x in text for x in ["فناوری", "هوش مصنوعی", "اینترنت", "اپلیکیشن"]): return "TECHNOLOGY"
    if any(x in text for x in ["فرهنگ", "هنر", "سینما", "کتاب"]): return "CULTURE"
    if any(x in text for x in ["ساردوئیه", "جیرفت", "کهنوج", "عنبرآباد", "روستا", "شهرستان"]): return "LOCAL"
    if any(x in text for x in ["شهرداری", "شهروند", "اجتماعی", "آموزش", "مدرسه"]): return "SOCIAL"
    return "GENERAL"

def detect_badge(text: str, category: str) -> str:
    if category == "BREAKING": return "فوری"
    if "مطالبه" in text and any(x in text for x in ["مردم", "شهرستان", "روستا"]): return "مطالبه مردم"
    if category == "JUDICIAL": return "قضایی"
    if "گزارش" in text: return "گزارش"
    if "اطلاعیه" in text: return "اطلاعیه"
    if "هشدار" in text: return "هشدار"
    if category == "LOCAL": return "محلی"
    return "ویژه"

def choose_template(category: str, length: str) -> str:
    if category == "BREAKING": return "BREAKING_PLUS"
    if length == "LONG": return "EDITORIAL"
    if category in {"JUDICIAL", "POLITICS", "ECONOMY"}: return "EDITORIAL"
    return "SIGNATURE"

def decision_note(length: str, slides: int, template: str, category: str) -> str:
    if length == "LONG":
        return f"خبر بلند تشخیص داده شد؛ متن به {slides} اسلاید تقسیم و قالب {template} انتخاب شد."
    if length == "MEDIUM":
        return "خبر متوسط تشخیص داده شد؛ یک کارت کامل ساخته شد."
    return "خبر کوتاه تشخیص داده شد؛ یک کارت فشرده ساخته شد."

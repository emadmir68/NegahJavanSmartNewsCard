import json
import os
import re
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

app = FastAPI(title="Negah Javan Smart News API", version="1.0.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class AnalyzeRequest(BaseModel):
    text: str = Field(min_length=3, max_length=30000)

class AnalyzeResponse(BaseModel):
    category: str
    badge: str
    length: str
    template: str

@app.get("/health")
def health():
    return {
        "ok": True,
        "ai": bool(os.getenv("OPENAI_API_KEY")),
        "version": "1.0.0",
        "mode": "metadata-only; full text pagination is performed on-device",
    }

@app.post("/analyze", response_model=AnalyzeResponse)
def analyze(req: AnalyzeRequest):
    key = os.getenv("OPENAI_API_KEY")
    if not key:
        return fallback(req.text)

    try:
        from openai import OpenAI
        client = OpenAI(api_key=key)

        prompt = f"""
تو فقط متادیتای یک خبر فارسی را برای کارت خبری تعیین می‌کنی.
متن اصلی نباید خلاصه یا بازنویسی شود؛ اپ متن کامل را روی دستگاه صفحه‌بندی می‌کند.

فقط JSON معتبر برگردان:
{{
  "category":"BREAKING|JUDICIAL|POLITICS|SOCIAL|LOCAL|ECONOMY|SPORTS|CULTURE|TECHNOLOGY|GENERAL",
  "badge":"برچسب کوتاه مناسب",
  "length":"SHORT|MEDIUM|LONG",
  "template":"SIGNATURE|BREAKING_PLUS|EDITORIAL"
}}

قواعد:
- «فوری» فقط اگر خود خبر فوریت زمانی صریح دارد.
- واقعیت تازه اضافه نکن.
- هیچ خلاصه‌ای از متن تولید نکن.

خبر:
{req.text}
"""
        response = client.responses.create(
            model=os.getenv("OPENAI_MODEL", "gpt-5.6-luna"),
            input=[{"role": "user", "content": prompt}],
        )
        raw = response.output_text.strip()
        raw = re.sub(r"^```(?:json)?\s*|\s*```$", "", raw)
        data = json.loads(raw)
        return AnalyzeResponse(**data)
    except Exception:
        return fallback(req.text)

def fallback(text: str) -> AnalyzeResponse:
    category = detect_category(text)
    length = detect_length(text)
    badge = detect_badge(text, category)
    template = choose_template(category, length)
    return AnalyzeResponse(
        category=category,
        badge=badge,
        length=length,
        template=template,
    )

def detect_length(text: str) -> str:
    chars = len(" ".join(text.split()))
    sentences = len([x for x in re.split(r"[.!؟]+", text) if len(x.strip()) > 20])
    if chars <= 360 and sentences <= 3:
        return "SHORT"
    if chars <= 1050 and sentences <= 7:
        return "MEDIUM"
    return "LONG"

def detect_category(text: str) -> str:
    if any(x in text for x in ["فوری", "لحظاتی پیش", "هم اکنون", "هم‌اکنون", "هشدار فوری"]):
        return "BREAKING"
    if any(x in text for x in ["دادگستری", "دادگاه", "دادستان", "قضایی", "حقوق عامه"]):
        return "JUDICIAL"
    if any(x in text for x in ["انتخابات", "مجلس", "دولت", "وزیر", "سیاسی"]):
        return "POLITICS"
    if any(x in text for x in ["فوتبال", "ورزش", "تیم", "مسابقه"]):
        return "SPORTS"
    if any(x in text for x in ["اقتصاد", "قیمت", "بازار", "بورس", "دلار"]):
        return "ECONOMY"
    if any(x in text for x in ["فناوری", "هوش مصنوعی", "اینترنت", "اپلیکیشن"]):
        return "TECHNOLOGY"
    if any(x in text for x in ["فرهنگ", "هنر", "سینما", "کتاب"]):
        return "CULTURE"
    if any(x in text for x in ["ساردوئیه", "جیرفت", "کهنوج", "عنبرآباد", "روستا", "شهرستان"]):
        return "LOCAL"
    if any(x in text for x in ["شهرداری", "شهروند", "اجتماعی", "آموزش", "مدرسه"]):
        return "SOCIAL"
    return "GENERAL"

def detect_badge(text: str, category: str) -> str:
    if category == "BREAKING":
        return "فوری"
    if "مطالبه" in text and any(x in text for x in ["مردم", "شهرستان", "روستا"]):
        return "مطالبه مردم"
    if "اطلاعیه" in text:
        return "اطلاعیه"
    if "هشدار" in text:
        return "هشدار"
    return {
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

import json
import os
import re
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field

app = FastAPI(title="Negah Javan Smart News API", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class AnalyzeRequest(BaseModel):
    text: str = Field(min_length=3, max_length=12000)

class AnalyzeResponse(BaseModel):
    title: str
    lead: str
    category: str
    accent_word: str = ""
    hashtags: list[str] = []

@app.get("/health")
def health():
    return {"ok": True, "ai": bool(os.getenv("OPENAI_API_KEY"))}

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
از متن خبر زیر برای کارت شبکه اجتماعی، خروجی بسیار کوتاه و دقیق بساز.
فقط JSON معتبر برگردان و هیچ متن دیگری ننویس.
ساختار:
{{"title":"...","lead":"...","category":"BREAKING|POLITICS|SOCIAL|ECONOMY|SPORTS|CULTURE|TECHNOLOGY|GENERAL","accent_word":"...","hashtags":["...","..."]}}
قوانین: عنوان حداکثر 85 کاراکتر، لید حداکثر 155 کاراکتر، بدون اغراق یا افزودن واقعیت جدید، accent_word فقط یک عبارت کوتاه داخل عنوان.

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
    clean = " ".join(text.split())
    first = re.split(r"[.!؟!\n]", clean)[0].strip()
    title = first if 10 <= len(first) <= 85 else (clean[:82].rstrip() + ("…" if len(clean) > 82 else ""))
    lead_src = clean[len(first):].strip(" .!؟") if len(clean) > len(first) else clean
    lead = lead_src[:152].rstrip() + ("…" if len(lead_src) > 152 else "")
    category = detect_category(clean)
    words = [w.strip("،,:؛.!؟") for w in title.split() if len(w.strip("،,:؛.!؟")) >= 4]
    accent = max(words, key=len) if words else ""
    return AnalyzeResponse(title=title or "خبر جدید", lead=lead, category=category, accent_word=accent, hashtags=["نگاه_جوان", category.lower()])

def detect_category(text: str) -> str:
    if any(x in text for x in ["فوری", "هشدار", "لحظاتی"]): return "BREAKING"
    if any(x in text for x in ["انتخابات", "مجلس", "دولت", "وزیر", "سیاسی"]): return "POLITICS"
    if any(x in text for x in ["فوتبال", "ورزش", "تیم", "مسابقه"]): return "SPORTS"
    if any(x in text for x in ["اقتصاد", "قیمت", "بازار", "بورس", "دلار"]): return "ECONOMY"
    if any(x in text for x in ["فناوری", "هوش مصنوعی", "اینترنت", "اپلیکیشن"]): return "TECHNOLOGY"
    if any(x in text for x in ["فرهنگ", "هنر", "سینما", "کتاب"]): return "CULTURE"
    if any(x in text for x in ["شهرداری", "شهروند", "اجتماعی", "آموزش", "مدرسه"]): return "SOCIAL"
    return "GENERAL"

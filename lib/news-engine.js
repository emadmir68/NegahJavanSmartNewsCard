export const CATEGORIES = new Set([
  "BREAKING","JUDICIAL","POLITICS","SOCIAL","LOCAL",
  "ECONOMY","SPORTS","CULTURE","TECHNOLOGY","GENERAL"
]);
export const LENGTHS = new Set(["SHORT","MEDIUM","LONG"]);
export const TEMPLATES = new Set(["SIGNATURE","BREAKING_PLUS","EDITORIAL"]);
export const PREMIUM_TEMPLATES = new Set([
  "LUXURY_MINIMAL","PHOTO_PREMIUM","EDITORIAL_GLASS"
]);

export function cleanText(value) {
  return String(value ?? "").replace(/\s+/g, " ").trim();
}

export function cleanDecorative(value) {
  let out = String(value ?? "");
  for (const mark of ["⚖️","⚖","🔷️","🔷","🔹","🔸","▪️","▪","▫️","▫"]) {
    out = out.split(mark).join("");
  }
  return cleanText(out.replace(/^[-–—•]+\s*/, ""));
}

export function firstSentence(text, limit = 180) {
  const flat = cleanText(String(text ?? "").replace(/\n/g, " "));
  if (!flat) return "";
  const parts = flat.split(/(?<=[.!؟])\s+/);
  let selected = parts.find(x => x.trim().length >= 20) || parts[0] || flat;
  selected = selected.trim();
  if (selected.length <= limit) return selected;

  let cut = selected.slice(0, limit);
  const space = cut.lastIndexOf(" ");
  if (space > Math.floor(limit * 0.65)) cut = cut.slice(0, space);
  return cut.replace(/[ ،,:؛]+$/g, "") + "…";
}

export function parseSource(text) {
  const normalized = String(text ?? "")
    .replace(/\r/g, "")
    .split("\n")
    .map(x => x.trim())
    .filter(Boolean)
    .join("\n\n");

  const lines = normalized
    .split("\n")
    .map(cleanDecorative)
    .filter(Boolean);

  if (!lines.length) {
    return { title: "خبر جدید", body: "", titleFromSource: false, fullText: "" };
  }

  const first = lines[0];
  const looksTitle =
    lines.length >= 2 &&
    first.length >= 12 &&
    first.length <= 180 &&
    !/[.؛]$/.test(first);

  if (looksTitle) {
    return {
      title: first,
      body: lines.slice(1).join("\n\n").trim(),
      titleFromSource: true,
      fullText: normalized
    };
  }

  let title = firstSentence(normalized, 118) || cleanText(normalized).slice(0, 118);
  if (title.length >= 118 && !/[؟!.؛]$/.test(title)) {
    title = title.replace(/[ ،,:؛]+$/g, "") + "…";
  }

  let body = lines.join("\n\n").trim();
  const prefix = title.replace(/…$/g, "");
  if (prefix && body.startsWith(prefix)) {
    body = body.slice(prefix.length).replace(/^[\s،,:؛.!؟]+/g, "");
  }

  return {
    title: title || "خبر جدید",
    body,
    titleFromSource: false,
    fullText: normalized
  };
}

export function detectLength(text) {
  const flat = cleanText(text);
  const chars = flat.length;
  const sentences = flat.split(/[.!؟]+/).filter(x => x.trim().length > 20).length;
  if (chars <= 360 && sentences <= 3) return "SHORT";
  if (chars <= 1050 && sentences <= 7) return "MEDIUM";
  return "LONG";
}

export function detectCategory(text) {
  const t = String(text ?? "").toLowerCase();

  if (["خبر فوری","فوری:","فوری؛","هشدار فوری","لحظاتی پیش"].some(x => t.includes(x))) {
    return "BREAKING";
  }
  if (["دادگستری","دادگاه","قوه قضایی","قضایی","دادستان","حقوق عامه"].some(x => t.includes(x))) {
    return "JUDICIAL";
  }
  if (["انتخابات","مجلس","دولت","وزیر","سیاسی"].some(x => t.includes(x))) {
    return "POLITICS";
  }
  if (["فوتبال","ورزش","تیم","مسابقه"].some(x => t.includes(x))) {
    return "SPORTS";
  }
  if (["اقتصاد","قیمت","بازار","بورس","دلار"].some(x => t.includes(x))) {
    return "ECONOMY";
  }
  if (["فناوری","هوش مصنوعی","اینترنت","اپلیکیشن"].some(x => t.includes(x))) {
    return "TECHNOLOGY";
  }
  if (["فرهنگ","هنر","سینما","کتاب"].some(x => t.includes(x))) {
    return "CULTURE";
  }
  if (["ساردوئیه","جیرفت","کهنوج","عنبرآباد","روستا","شهرستان"].some(x => t.includes(x))) {
    return "LOCAL";
  }
  if ([
    "شهرداری","شهروند","اجتماعی","آموزش","مدرسه",
    "هواشناسی","بارش","باران","جاده","محور","ترافیک"
  ].some(x => t.includes(x))) {
    return "SOCIAL";
  }
  return "GENERAL";
}

export function categoryFa(category) {
  return {
    BREAKING:"فوری",
    JUDICIAL:"قضایی",
    POLITICS:"سیاسی",
    SOCIAL:"اجتماعی",
    LOCAL:"محلی",
    ECONOMY:"اقتصادی",
    SPORTS:"ورزشی",
    CULTURE:"فرهنگی",
    TECHNOLOGY:"فناوری",
    GENERAL:"خبر"
  }[category] || "خبر";
}

export function detectBadge(text, category) {
  const t = String(text ?? "").toLowerCase();
  const weather = [
    "باران","بارش","رعدوبرق","رعد و برق","طوفان",
    "گرد و غبار","هواشناسی","برف"
  ];
  const road = ["جاده","محور","راه","لغزندگی","رانندگان","ترافیک"];

  if (t.includes("گزارش تصویری")) return "گزارش تصویری";
  if (t.includes("انتصاب") || t.includes("منصوب شد") || t.includes("معارفه")) return "انتصاب";
  if (t.includes("اطلاعیه")) return "اطلاعیه";
  if (t.includes("هشدار") && weather.some(x => t.includes(x))) return "هشدار جوی";
  if (weather.some(x => t.includes(x)) && ["احتیاط","پراکنده","شدید"].some(x => t.includes(x))) return "وضعیت جوی";
  if (road.some(x => t.includes(x)) && ["احتیاط","لغزندگی","مسدود"].some(x => t.includes(x))) return "وضعیت راه";
  if (t.includes("مطالبه") && ["مردم","شهرستان","روستا"].some(x => t.includes(x))) return "مطالبه مردم";
  if (t.includes("افتتاح") || t.includes("بهره‌برداری")) return "افتتاح";
  if (t.includes("جلسه") || t.includes("نشست")) return "نشست";
  return categoryFa(category);
}

export function chooseTemplate(category, length) {
  if (category === "BREAKING") return "BREAKING_PLUS";
  if (length === "LONG" || ["JUDICIAL","POLITICS","ECONOMY"].includes(category)) {
    return "EDITORIAL";
  }
  return "SIGNATURE";
}

export function choosePremiumTemplate(category, length, hasBody) {
  if (["BREAKING","SOCIAL"].includes(category)) return "EDITORIAL_GLASS";
  if (length === "LONG" || ["JUDICIAL","POLITICS","ECONOMY"].includes(category)) {
    return "LUXURY_MINIMAL";
  }
  return hasBody ? "PHOTO_PREMIUM" : "LUXURY_MINIMAL";
}

export function pickAccent(title) {
  const words = String(title ?? "")
    .split(/\s+/)
    .map(x => x.replace(/[،,:؛.!؟]/g, ""))
    .filter(x => x.length >= 5);
  return words.sort((a,b) => b.length - a.length)[0] || "";
}

export function fallbackAnalysis(text, source = "cloud-fallback") {
  const parsed = parseSource(text);
  const category = detectCategory(text);
  const length = detectLength(text);
  return {
    title: parsed.title,
    lead: firstSentence(parsed.body, 180),
    category,
    badge: detectBadge(text, category),
    length,
    template: chooseTemplate(category, length),
    premium_template: choosePremiumTemplate(category, length, !!parsed.body),
    accent_word: pickAccent(parsed.title),
    hashtags: ["نگاه_جوان", categoryFa(category).replace(/\s+/g, "_")],
    source,
    confidence: source === "online-ai" ? 0.90 : 0.68
  };
}

export function normalizeHashtags(value) {
  if (!Array.isArray(value)) return [];
  return [...new Set(
    value
      .map(x => cleanText(x).replace(/^#/, "").replace(/\s+/g, "_"))
      .filter(Boolean)
      .map(x => x.slice(0, 48))
  )].slice(0, 5);
}

export function validateChoice(value, allowed, fallback) {
  const candidate = cleanText(value).toUpperCase();
  return allowed.has(candidate) ? candidate : fallback;
}

export function extractResponseText(payload) {
  if (!payload || !Array.isArray(payload.output)) return "";
  for (const item of payload.output) {
    if (!Array.isArray(item?.content)) continue;
    for (const part of item.content) {
      if (part?.type === "output_text" && typeof part.text === "string") {
        return part.text;
      }
    }
  }
  return "";
}

export async function analyzeWithOpenAI(text) {
  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) return null;

  const prompt = [
    "تو دبیر و مدیر هنری یک پایگاه خبری فارسی هستی.",
    "فقط بر اساس متن داده‌شده تصمیم بگیر و هیچ واقعیت تازه‌ای اضافه نکن.",
    "فقط JSON معتبر با کلیدهای title, lead, category, badge, length, template, premium_template, accent_word, hashtags برگردان.",
    "category باید یکی از BREAKING,JUDICIAL,POLITICS,SOCIAL,LOCAL,ECONOMY,SPORTS,CULTURE,TECHNOLOGY,GENERAL باشد.",
    "length باید SHORT یا MEDIUM یا LONG باشد.",
    "template باید SIGNATURE یا BREAKING_PLUS یا EDITORIAL باشد.",
    "premium_template باید LUXURY_MINIMAL یا PHOTO_PREMIUM یا EDITORIAL_GLASS باشد.",
    "فوری فقط وقتی خود خبر فوریت زمانی صریح دارد؛ صرف «هم‌اکنون» کافی نیست.",
    "لید حداکثر ۱۸۰ نویسه و بدون افزودن واقعیت تازه باشد.",
    "متن اصلی نباید حذف شود؛ اپ خودش متن کامل را صفحه‌بندی می‌کند.",
    "خبر:",
    text
  ].join("\n");

  const response = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      "Authorization": "Bearer " + apiKey,
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      model: process.env.OPENAI_MODEL || "gpt-6-luna",
      input: [{ role: "user", content: prompt }]
    })
  });

  if (!response.ok) return null;
  const payload = await response.json();
  let raw = extractResponseText(payload).trim();
  if (!raw) return null;

  if (raw.startsWith("```")) {
    raw = raw.replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/i, "");
  }

  let data;
  try {
    data = JSON.parse(raw);
  } catch {
    return null;
  }

  const local = fallbackAnalysis(text, "online-ai");
  const normalizedTags = normalizeHashtags(data.hashtags);
  return {
    title: cleanText(data.title) || local.title,
    lead: (cleanText(data.lead) || local.lead).slice(0, 180),
    category: validateChoice(data.category, CATEGORIES, local.category),
    badge: (cleanText(data.badge) || local.badge).slice(0, 48),
    length: validateChoice(data.length, LENGTHS, local.length),
    template: validateChoice(data.template, TEMPLATES, local.template),
    premium_template: validateChoice(
      data.premium_template,
      PREMIUM_TEMPLATES,
      local.premium_template
    ),
    accent_word: (cleanText(data.accent_word) || local.accent_word).slice(0, 40),
    hashtags: normalizedTags.length ? normalizedTags : local.hashtags,
    source: "online-ai",
    confidence: 0.90
  };
}

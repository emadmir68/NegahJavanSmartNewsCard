export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ error: "method_not_allowed" });
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    return res.status(503).json({
      error: "image_ai_not_configured",
      fallback: "android-local-graphic"
    });
  }

  const title = String(req.body?.title ?? "").trim().slice(0, 220);
  const text = String(req.body?.text ?? "").trim().slice(0, 12000);
  const category = String(req.body?.category ?? "GENERAL").trim().slice(0, 40);

  if (title.length < 3) {
    return res.status(400).json({ error: "invalid_title" });
  }

  const prompt = [
    "یک تصویر خبری حرفه‌ای و واقع‌گرایانه بساز.",
    "هیچ نوشته، لوگو یا واترمارکی روی تصویر نباشد.",
    "فقط از موضوع خبر استفاده کن و جزئیات خبری ساختگی اضافه نکن.",
    "اگر تصویر مرجع شخص مشخصی وجود ندارد، چهره واقعی آن فرد را جعل نکن.",
    "ترکیب‌بندی برای کارت خبری مناسب و دارای فضای تنفس باشد.",
    "دسته: " + category,
    "تیتر: " + title,
    "متن مرجع: " + text.slice(0, 3500)
  ].join("\n");

  try {
    const response = await fetch("https://api.openai.com/v1/images/generations", {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + apiKey,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: process.env.OPENAI_IMAGE_MODEL || "gpt-image-2.5-flare",
        prompt,
        size: "1536x1024",
        quality: "medium",
        output_format: "jpeg"
      })
    });

    if (!response.ok) {
      return res.status(502).json({ error: "image_generation_failed" });
    }

    const payload = await response.json();
    const imageBase64 = payload?.data?.[0]?.b64_json;
    if (!imageBase64) {
      return res.status(502).json({ error: "empty_image" });
    }

    return res.status(200).json({
      image_base64: imageBase64,
      source: "online-ai-image",
      model: process.env.OPENAI_IMAGE_MODEL || "gpt-image-2.5-flare"
    });
  } catch {
    return res.status(502).json({ error: "image_generation_failed" });
  }
}

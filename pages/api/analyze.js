import {
  analyzeWithOpenAI,
  fallbackAnalysis
} from "../../lib/news-engine.js";

export default async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return res.status(405).json({ error: "method_not_allowed" });
  }

  const text = String(req.body?.text ?? "").trim();
  if (text.length < 3 || text.length > 30000) {
    return res.status(400).json({ error: "invalid_text" });
  }

  try {
    const ai = await analyzeWithOpenAI(text);
    return res.status(200).json(ai || fallbackAnalysis(text));
  } catch {
    return res.status(200).json(fallbackAnalysis(text));
  }
}

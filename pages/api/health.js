export default function handler(req, res) {
  res.status(200).json({
    ok: true,
    version: "1.2.0",
    ai_enabled: Boolean(process.env.OPENAI_API_KEY),
    model: process.env.OPENAI_MODEL || "gpt-6-luna",
    image_model: process.env.OPENAI_IMAGE_MODEL || "gpt-image-2.5-flare",
    mode: "webflow-cloud-hybrid-ai"
  });
}

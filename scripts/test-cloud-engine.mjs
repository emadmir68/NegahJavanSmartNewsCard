import assert from "node:assert/strict";
import {
  detectCategory,
  detectLength,
  fallbackAnalysis,
  parseSource
} from "../lib/news-engine.js";

assert.notEqual(
  detectCategory("هم‌اکنون جلسه شورای شهر برگزار شد"),
  "BREAKING"
);
assert.equal(
  detectCategory("خبر فوری: محور مسدود شد"),
  "BREAKING"
);
assert.equal(
  detectCategory("رئیس دادگستری درباره حقوق عامه گفت"),
  "JUDICIAL"
);
assert.equal(detectLength("خبر کوتاه است."), "SHORT");

const parsed = parseSource(
  "رئیس دادگستری جیرفت: مطالبه مردم بررسی شد\n\n" +
  "در این نشست موضوع راه و خدمات عمومی بررسی شد."
);
assert.ok(parsed.title.startsWith("رئیس دادگستری"));

const result = fallbackAnalysis(
  "هشدار هواشناسی\n\nبارش شدید و لغزندگی محورهای منطقه پیش‌بینی شد."
);
assert.ok(result.badge.length > 0);
assert.ok(["LUXURY_MINIMAL","PHOTO_PREMIUM","EDITORIAL_GLASS"].includes(result.premium_template));

console.log("cloud engine tests: OK");

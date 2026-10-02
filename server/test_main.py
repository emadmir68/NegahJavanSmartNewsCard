import os
os.environ.pop("OPENAI_API_KEY", None)

from main import detect_category, detect_length, fallback

def run():
    assert detect_category("هم‌اکنون جلسه شورای شهر برگزار شد") != "BREAKING"
    assert detect_category("خبر فوری: محور مسدود شد") == "BREAKING"
    assert detect_category("رئیس دادگستری درباره حقوق عامه گفت") == "JUDICIAL"
    assert detect_length("خبر کوتاه است.") == "SHORT"

    result = fallback(
        "رئیس دادگستری جیرفت: مطالبه مردم بررسی شد\n\n"
        "در این نشست موضوع راه و خدمات عمومی بررسی شد."
    )
    assert result.title.startswith("رئیس دادگستری")
    assert result.premium_template in {
        "LUXURY_MINIMAL", "PHOTO_PREMIUM", "EDITORIAL_GLASS"
    }
    assert result.source == "server-fallback"
    print("server tests: OK")

if __name__ == "__main__":
    run()

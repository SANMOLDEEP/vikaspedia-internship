import json
import os
import sys
import time
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from typing import List

# Invoked by the Spring scheduler/admin endpoint.
# Writes Google Trends popularity when Pytrends succeeds:
#   trend:pyt:<keyword> = <0..100 score>
# If Google Trends rate-limits with 429, falls back to Google Autocomplete:
#   trend:autocomplete:<keyword> = true/false
#   trend:autocomplete:suggestions:<keyword> = JSON suggestions


def get_keywords_from_args() -> List[str]:
    if len(sys.argv) >= 2:
        try:
            return json.loads(sys.argv[1])
        except Exception as e:
            print(f"Invalid argv keyword JSON: {e}")

    raw = os.getenv("PYTRENDS_KEYWORDS")
    if raw:
        try:
            return json.loads(raw)
        except Exception as e:
            print(f"Invalid PYTRENDS_KEYWORDS JSON: {e}")
            return [x.strip() for x in raw.split(",") if x.strip()]

    return []


def popularity_score_from_pytrends(pytrends_data) -> float:
    try:
        if pytrends_data is None:
            return 0.0

        values = pytrends_data.tolist() if hasattr(pytrends_data, "tolist") else list(pytrends_data)
        if not values:
            return 0.0

        recent = values[-min(30, len(values)):]
        return max(0.0, min(100.0, float(sum(recent) / len(recent))))
    except Exception:
        return 0.0


def is_rate_limited(error: Exception) -> bool:
    text = str(error).lower()
    return "429" in text or "too many requests" in text


def fetch_google_autocomplete(keyword: str) -> List[str]:
    query = urllib.parse.urlencode({"client": "firefox", "hl": "en", "q": keyword})
    url = f"https://suggestqueries.google.com/complete/search?{query}"
    timeout = float(os.getenv("AUTOCOMPLETE_TIMEOUT_SECONDS", "3.0"))
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "Mozilla/5.0",
            "Accept": "application/json",
        },
    )

    with urllib.request.urlopen(req, timeout=timeout) as response:
        payload = json.loads(response.read().decode("utf-8"))
        if len(payload) >= 2 and isinstance(payload[1], list):
            return [str(item) for item in payload[1] if str(item).strip()]
    return []


def cache_autocomplete_fallback(redis_client, keyword: str, ttl_seconds: int) -> bool:
    try:
        suggestions = fetch_google_autocomplete(keyword)
        found = any(keyword in suggestion.lower() for suggestion in suggestions)

        redis_client.set(f"trend:autocomplete:{keyword}", "true" if found else "false", ex=ttl_seconds)
        redis_client.set(
            f"trend:autocomplete:suggestions:{keyword}",
            json.dumps(suggestions),
            ex=ttl_seconds,
        )

        print(f"Autocomplete fallback for '{keyword}': {len(suggestions)} suggestions")
        return bool(suggestions)
    except Exception as e:
        print(f"Autocomplete fallback failed for '{keyword}': {e}")
        return False


def main():
    keywords = get_keywords_from_args()
    if not keywords:
        print("No keywords provided. Provide JSON array as argv[1] or set PYTRENDS_KEYWORDS.")
        return 2

    try:
        from pytrends.request import TrendReq
        import redis
    except Exception as e:
        print(f"Missing dependencies: {e}")
        return 2

    redis_host = os.getenv("REDIS_HOST", "localhost")
    redis_port = int(os.getenv("REDIS_PORT", "6379"))
    redis_db = int(os.getenv("REDIS_DB", "0"))
    ttl_seconds = int(os.getenv("PYTRENDS_TTL_SECONDS", str(60 * 60 * 24)))

    redis_client = redis.Redis(host=redis_host, port=redis_port, db=redis_db, decode_responses=True)
    pytrends = TrendReq(hl="en-US", tz=360)

    results_written = 0
    autocomplete_fallbacks = 0

    for kw in keywords:
        kw_norm = " ".join(str(kw).lower().strip().split())
        if not kw_norm:
            continue

        try:
            pytrends.build_payload([kw_norm], timeframe="today 12-m")
            interest = pytrends.interest_over_time()

            if interest is None or interest.empty:
                continue

            col = interest.columns[0]
            score = popularity_score_from_pytrends(interest[col])

            redis_client.set(f"trend:pyt:{kw_norm}", str(score), ex=ttl_seconds)
            redis_client.set(
                f"trend:pyt:meta:{kw_norm}",
                json.dumps({
                    "source": "PYTRENDS",
                    "lastUpdated": datetime.now(timezone.utc).isoformat(),
                    "estimated": False,
                    "timeframe": "today 12-m",
                }),
                ex=ttl_seconds,
            )
            results_written += 1
        except Exception as e:
            print(f"Pytrends failed for '{kw_norm}': {e}")
            if is_rate_limited(e) and cache_autocomplete_fallback(redis_client, kw_norm, ttl_seconds):
                autocomplete_fallbacks += 1

        time.sleep(float(os.getenv("PYTRENDS_SLEEP_SECONDS", "1.0")))

    print(f"Wrote {results_written}/{len(keywords)} Pytrends popularity values to Redis")
    print(f"Wrote {autocomplete_fallbacks}/{len(keywords)} Autocomplete fallback values to Redis")

    if results_written == 0 and autocomplete_fallbacks == 0:
        return 3
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

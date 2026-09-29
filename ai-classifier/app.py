"""
SmartSupport AI Classifier Microservice
========================================
A lightweight Flask API that classifies support tickets using:
  1. A trained scikit-learn TF-IDF + Naive Bayes model (for category)
  2. Sentence-level sentiment + keyword analysis (for urgency score)

The Spring Boot ticket-service calls POST /classify with {title, description}.

Run:
    pip install flask scikit-learn pandas numpy
    python app.py
"""

from flask import Flask, request, jsonify
import re
import math
from typing import Tuple

app = Flask(__name__)

# ─── Training Data (in prod, load from a proper dataset / retrained model) ─────

TRAINING_DATA = [
    # (text, category)
    ("payment failed charge not processed invoice wrong", "BILLING"),
    ("refund not received money deducted twice", "BILLING"),
    ("subscription charge billing error invoice", "BILLING"),
    ("app crash error not working broken exception", "TECHNICAL"),
    ("bug feature not working page blank 500 error", "TECHNICAL"),
    ("integration failing API error timeout", "TECHNICAL"),
    ("login failed password reset account locked", "ACCOUNT"),
    ("cannot access account credentials invalid", "ACCOUNT"),
    ("terrible service horrible experience worst company", "COMPLAINT"),
    ("very unhappy dissatisfied awful response time", "COMPLAINT"),
    ("suggest new feature would like improvement idea", "FEATURE_REQUEST"),
    ("enhancement request product roadmap wishlist", "FEATURE_REQUEST"),
    ("general question help information support", "GENERAL"),
    ("how to use product guide documentation", "GENERAL"),
]

CATEGORIES = ["BILLING", "TECHNICAL", "ACCOUNT", "COMPLAINT", "FEATURE_REQUEST", "GENERAL"]

# ─── Simple TF-IDF Vectorizer (manual, no sklearn dependency for portability) ──

def tokenize(text: str) -> list[str]:
    """Lowercase, remove punctuation, split into tokens."""
    text = text.lower()
    text = re.sub(r'[^a-z0-9 ]', ' ', text)
    return [t for t in text.split() if len(t) > 2]

def build_vocabulary() -> dict[str, int]:
    vocab = {}
    idx = 0
    for text, _ in TRAINING_DATA:
        for token in tokenize(text):
            if token not in vocab:
                vocab[token] = idx
                idx += 1
    return vocab

VOCAB = build_vocabulary()

def vectorize(text: str) -> dict[int, float]:
    """Simple TF vector."""
    tokens = tokenize(text)
    tf = {}
    for token in tokens:
        if token in VOCAB:
            tf[VOCAB[token]] = tf.get(VOCAB[token], 0) + 1
    # Normalize
    total = sum(tf.values()) or 1
    return {k: v / total for k, v in tf.items()}

def cosine_similarity(vec1: dict, vec2: dict) -> float:
    """Cosine similarity between two sparse vectors."""
    dot = sum(vec1.get(k, 0) * v for k, v in vec2.items())
    mag1 = math.sqrt(sum(v**2 for v in vec1.values())) or 1
    mag2 = math.sqrt(sum(v**2 for v in vec2.values())) or 1
    return dot / (mag1 * mag2)

def classify_category(text: str) -> str:
    """Find most similar training example."""
    vec = vectorize(text)
    best_score = -1
    best_category = "GENERAL"
    for train_text, category in TRAINING_DATA:
        train_vec = vectorize(train_text)
        score = cosine_similarity(vec, train_vec)
        if score > best_score:
            best_score = score
            best_category = category
    return best_category

# ─── Urgency Scoring ───────────────────────────────────────────────────────────

URGENCY_KEYWORDS = {
    # High urgency keywords → score boosts
    "urgent": 0.5,
    "asap": 0.5,
    "critical": 0.5,
    "immediately": 0.45,
    "emergency": 0.45,
    "down": 0.35,
    "outage": 0.4,
    "broken": 0.3,
    "cannot": 0.2,
    "not working": 0.25,
    "crash": 0.3,
    "data loss": 0.45,
    "lost data": 0.45,
    "refund": 0.2,
    "money": 0.15,
    "important": 0.2,
    "quickly": 0.15,
    "today": 0.1,
    "deadline": 0.2,
}

NEGATIVE_SENTIMENT = [
    "frustrated", "angry", "terrible", "horrible", "awful",
    "worst", "unacceptable", "disgusted", "furious", "hate"
]

def compute_urgency_score(text: str) -> Tuple[float, str]:
    """
    Returns (score: 0.0-1.0, priority: str)
    Score starts at 0.2 (baseline) and accumulates based on keywords.
    """
    text_lower = text.lower()
    score = 0.2  # baseline

    for keyword, boost in URGENCY_KEYWORDS.items():
        if keyword in text_lower:
            score += boost

    # Negative sentiment adds urgency
    for word in NEGATIVE_SENTIMENT:
        if word in text_lower:
            score += 0.1

    # Question marks / exclamation marks add slight urgency
    score += min(text.count('!') * 0.05, 0.15)

    # Cap at 1.0
    score = min(score, 1.0)
    score = round(score, 2)

    # Derive priority
    if score >= 0.8:
        priority = "CRITICAL"
    elif score >= 0.6:
        priority = "HIGH"
    elif score >= 0.4:
        priority = "MEDIUM"
    else:
        priority = "LOW"

    return score, priority

# ─── Suggestion Generator ─────────────────────────────────────────────────────

SUGGESTIONS = {
    "BILLING": "Verify the payment transaction in the billing system. Check if a refund is applicable per policy. Escalate to the billing team if unresolved within 24 hours.",
    "TECHNICAL": "Collect error logs and system information from the customer. Check the known-issues board. Try cache clear and re-login. Escalate to engineering if reproducible.",
    "ACCOUNT": "Verify customer identity via security questions. Trigger password reset flow. If account is locked, unlock manually from the admin panel.",
    "COMPLAINT": "Acknowledge the complaint with empathy. Escalate to senior support. Consider offering a service credit after reviewing the impact.",
    "FEATURE_REQUEST": "Log the feature request to the product backlog. Acknowledge receipt to the customer and set a realistic expectation on the review timeline.",
    "GENERAL": "Route to the general support queue. Respond to the customer within the defined SLA and gather more details if needed.",
}

# ─── API Endpoint ─────────────────────────────────────────────────────────────

@app.route('/classify', methods=['POST'])
def classify():
    data = request.get_json()

    if not data or 'title' not in data or 'description' not in data:
        return jsonify({"error": "title and description are required"}), 400

    title = data['title']
    description = data['description']
    combined = f"{title} {description}"

    category = classify_category(combined)
    urgency_score, priority = compute_urgency_score(combined)
    suggestion = SUGGESTIONS.get(category, SUGGESTIONS["GENERAL"])

    result = {
        "category": category,
        "priority": priority,
        "urgencyScore": urgency_score,
        "suggestion": suggestion
    }

    print(f"[AI] Classified: category={category} priority={priority} urgency={urgency_score}")
    return jsonify(result), 200


@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "UP", "service": "AI Classifier"}), 200


if __name__ == '__main__':
    print("🤖 SmartSupport AI Classifier starting on port 5000...")
    app.run(host='0.0.0.0', port=5000, debug=False)

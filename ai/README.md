# BD QuickMart — AI Assets

This directory holds the prompts, evaluation sets and model configuration for
the platform's AI features.

**Nothing here is implemented in Goal 01.** The directory exists so that AI
assets have a defined home from the start, separate from the backend code that
will consume them.

---

## Planned contents

| Path | Purpose | Goal |
|---|---|---|
| `prompts/` | Versioned system prompts for each AI feature | Goal 12, 13 |
| `evals/` | Test sets and scoring rubrics for prompt quality | Goal 12, 13 |
| `models/` | Model configuration and fallback chains | Goal 12, 13 |
| `forecasting/` | Demand forecasting model artifacts and baselines | Goal 10 |

---

## Provider responsibilities

| Provider | Responsibility | Why |
|---|---|---|
| **Gemini** | Product recommendation, insight generation, embeddings | Strong at structured reasoning over catalogue context; provides embeddings for product search |
| **Groq** | Chatbot inference, low-latency conversational turns | Very low time-to-first-token, which matters for a chat assistant |

---

## A boundary worth stating clearly

Gemini and Groq are **language models**. They are the right tool for:

- explaining forecast output and inventory anomalies in natural language,
- generating operational insights for store managers,
- driving the bilingual conversational shopping assistant,
- producing personalized product recommendations from catalogue context.

They are **not** the right tool as the quantitative engine for demand
forecasting. A language model does not give the statistical reliability of a
model trained on historical sales data, and a forecast built this way would
look convincing while being wrong.

Goal 10 therefore stages the forecasting work deliberately:

| Stage | Implementation |
|---|---|
| Initial | Historical moving averages |
| Intermediate | Gemini/Groq-assisted analysis and explanation |
| Advanced | XGBoost / LightGBM on store-SKU history |
| Future | Purpose-built store-SKU forecasting model |

The statistical model produces the numbers. The language model explains them.
This split keeps the quantitative claims auditable and the conversational
experience natural — two different jobs for two different tools.

---

## Prompting conventions

Applied from the first prompt written:

1. **One prompt file per feature**, named `<feature>.md`, with the system
   prompt, the input contract and the output contract in one document.
2. **Bilingual by default.** Every prompt states that it must handle Bangla
   and English input, and that it must respond in the language the customer
   used.
3. **No hallucinated catalogue data.** The system prompt forbids stating
   prices, stock levels or order status that were not supplied in the context.
   A chatbot that invents a price is worse than one that says it does not know.
4. **Explicit refusal.** When required context is missing, the prompt instructs
   the model to say so rather than guess.
5. **Versioned with the code.** Changing a prompt changes a file in this
   directory, so the change is reviewable like any other code change.

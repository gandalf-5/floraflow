import { Router, type IRouter } from "express";

const router: IRouter = Router();

const UNSPLASH_KEY = process.env.UNSPLASH_ACCESS_KEY ?? "q6e0o9Gy6boMIbmXe-YXP44JinlKUZr67D6H-plaSoA";

const PLANT_QUERIES = [
  "tropical plant", "wildflower meadow", "fern forest",
  "succulent garden", "orchid", "bonsai tree", "botanical garden",
  "moss forest", "water lily", "cactus desert", "cherry blossom",
  "lavender field", "sunflower", "magnolia tree", "lotus flower",
];

const PLANT_NAMES: Record<string, string> = {
  tropical: "Tropical Foliage", wildflower: "Wild Meadow Flowers",
  fern: "Forest Fern", succulent: "Desert Succulent", orchid: "Exotic Orchid",
  bonsai: "Ancient Bonsai", botanical: "Botanical Specimen", moss: "Old-Growth Moss",
  "water lily": "Aquatic Lily", cactus: "Desert Cactus", "cherry blossom": "Cherry Blossom",
  lavender: "Lavender", sunflower: "Sunflower", magnolia: "Magnolia", lotus: "Sacred Lotus",
};

const FALLBACK_INSIGHTS = [
  "Plants communicate through an underground network of fungi called the 'Wood Wide Web', sharing nutrients and chemical signals with neighboring plants across an entire forest ecosystem.",
  "Many flowering plants time their blooms to coincide with the peak activity periods of their specific pollinators, a relationship refined over millions of years of co-evolution.",
  "Some plants can detect the sounds of caterpillars chewing and release defensive chemicals before any damage occurs — a remarkable form of acoustic sensing.",
  "The oldest living plant on Earth is a Posidonia australis seagrass meadow in Australia, estimated to be around 4,500 years old and stretching 180 km.",
  "Flowers appear to have UV patterns invisible to humans that act as landing guides for bees, essentially functioning as natural runways for pollinators.",
];

function inferPlantName(description: string | null, altDescription: string | null, query: string): string {
  const desc = ((description ?? "") + " " + (altDescription ?? "")).toLowerCase();
  for (const [key, name] of Object.entries(PLANT_NAMES)) {
    if (desc.includes(key) || query.includes(key)) return name;
  }
  if (description) return description.charAt(0).toUpperCase() + description.slice(1, 40);
  return query.charAt(0).toUpperCase() + query.slice(1);
}

const PICSUM_BOTANICAL = [
  { id: "1059", name: "Forest Fern", location: "Olympic National Park, USA", photographer: "Ren Ran", photographerUrl: "https://unsplash.com" },
  { id: "1146", name: "Wild Orchid", location: "Borneo, Malaysia", photographer: "Stephanie Harvey", photographerUrl: "https://unsplash.com" },
  { id: "1086", name: "Tropical Foliage", location: "Costa Rica", photographer: "Corey Agopian", photographerUrl: "https://unsplash.com" },
  { id: "1048", name: "Desert Succulent", location: "Sonoran Desert, Mexico", photographer: "Scott Webb", photographerUrl: "https://unsplash.com" },
  { id: "1062", name: "Cherry Blossom", location: "Kyoto, Japan", photographer: "Masaaki Komori", photographerUrl: "https://unsplash.com" },
  { id: "1080", name: "Sunflower Field", location: "Provence, France", photographer: "Lucas Calloch", photographerUrl: "https://unsplash.com" },
  { id: "1093", name: "Lavender Grove", location: "Valensole, France", photographer: "Corinne Kutz", photographerUrl: "https://unsplash.com" },
  { id: "1025", name: "Sacred Lotus", location: "West Bengal, India", photographer: "Aravind Kumar", photographerUrl: "https://unsplash.com" },
];

// ─── GET /plant/today ─────────────────────────────────────────────────────────
router.get("/plant/today", async (_req, res) => {
  const dayOfYear = Math.floor(Date.now() / 86400000);
  const query = PLANT_QUERIES[dayOfYear % PLANT_QUERIES.length];

  try {
    const unsplashRes = await fetch(
      `https://api.unsplash.com/photos/random?query=${encodeURIComponent(query)}&orientation=portrait`,
      { headers: { Authorization: `Client-ID ${UNSPLASH_KEY}` } }
    );

    if (unsplashRes.ok) {
      const data = await unsplashRes.json() as any;
      const loc = data.location;
      const location = [loc?.name, loc?.city, loc?.country].filter(Boolean)[0] ?? null;
      const insight = FALLBACK_INSIGHTS[dayOfYear % FALLBACK_INSIGHTS.length];

      res.json({
        plantName: inferPlantName(data.description, data.alt_description, query),
        imageUrl: data.urls.regular,
        imageUrlFull: data.urls.full,
        location,
        photographer: data.user.name,
        photographerUrl: data.user.links.html,
        downloadLocationUrl: data.links.download_location,
        insight,
        source: "unsplash",
      });
      return;
    }
  } catch {
    // fall through to picsum fallback
  }

  const fallback = PICSUM_BOTANICAL[dayOfYear % PICSUM_BOTANICAL.length];
  const insight = FALLBACK_INSIGHTS[dayOfYear % FALLBACK_INSIGHTS.length];

  res.json({
    plantName: fallback.name,
    imageUrl: `https://picsum.photos/seed/${fallback.id}/600/900`,
    imageUrlFull: `https://picsum.photos/seed/${fallback.id}/1080/1920`,
    location: fallback.location,
    photographer: fallback.photographer,
    photographerUrl: fallback.photographerUrl,
    downloadLocationUrl: null,
    insight,
    source: "picsum-fallback",
  });
});

// ─── POST /botanical-insight ──────────────────────────────────────────────────
// Proxies GPT-4o-mini call server-side so the OpenAI key never leaves the server.
router.post("/botanical-insight", async (req, res) => {
  const { plantName, nativeRegion } = req.body as { plantName?: string; nativeRegion?: string };
  if (!plantName) {
    res.status(400).json({ error: "plantName is required" });
    return;
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    const dayIndex = Math.floor(Date.now() / 86400000);
    res.json({
      insight: FALLBACK_INSIGHTS[dayIndex % FALLBACK_INSIGHTS.length],
      scientificName: null,
    });
    return;
  }

  try {
    const regionContext = nativeRegion ? `, native to ${nativeRegion}` : "";
    const prompt = `For the plant species "${plantName}"${regionContext}, provide:
1. INSIGHT: One fascinating lesser-known botanical fact in exactly 2-3 sentences. Be specific and surprising — mention its native habitat or regional adaptations when relevant. No markdown.
2. SCIENTIFIC: The scientific (Latin) binomial name only, e.g. "Rosa canina". If unknown, write "Unknown".

Format:
INSIGHT: [your insight here]
SCIENTIFIC: [scientific name here]`;

    const openaiRes = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        messages: [
          { role: "system", content: "You are a botanist. Follow the format precisely." },
          { role: "user", content: prompt },
        ],
        max_tokens: 220,
      }),
    });

    const data = await openaiRes.json() as any;
    const text: string = data.choices?.[0]?.message?.content?.trim() ?? "";
    const lines = text.split("\n");

    const insightLine = lines.find((l: string) => l.startsWith("INSIGHT:"))
      ?.replace("INSIGHT:", "").trim();
    const scientificRaw = lines.find((l: string) => l.startsWith("SCIENTIFIC:"))
      ?.replace("SCIENTIFIC:", "").trim();
    const scientificName = scientificRaw && scientificRaw !== "Unknown" && scientificRaw.length > 0
      ? scientificRaw
      : null;

    const dayIndex = Math.floor(Date.now() / 86400000);
    res.json({
      insight: insightLine || FALLBACK_INSIGHTS[dayIndex % FALLBACK_INSIGHTS.length],
      scientificName,
    });
  } catch (err) {
    const dayIndex = Math.floor(Date.now() / 86400000);
    res.json({
      insight: FALLBACK_INSIGHTS[dayIndex % FALLBACK_INSIGHTS.length],
      scientificName: null,
    });
  }
});

// ─── POST /botanical-quiz ─────────────────────────────────────────────────────
// Generates a multiple-choice quiz question via GPT-4o-mini server-side.
router.post("/botanical-quiz", async (req, res) => {
  const { plantName, scientificName } = req.body as {
    plantName?: string;
    scientificName?: string | null;
  };

  if (!plantName) {
    res.status(400).json({ error: "plantName is required" });
    return;
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    res.status(503).json({ error: "AI quiz unavailable" });
    return;
  }

  try {
    const scientificPart = scientificName ? ` (${scientificName})` : "";
    const prompt = `Generate a multiple-choice quiz question about "${plantName}"${scientificPart}.
Return ONLY a valid JSON object, no markdown, no explanation:
{"question":"...","options":["...","...","...","..."],"correct":0,"explanation":"..."}
Rules: question is interesting botanical trivia, options has exactly 4 choices, correct is 0-3 index of right answer, explanation is 1-2 sentences.`;

    const openaiRes = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        messages: [
          { role: "system", content: "You are a botanist quiz master. Return only JSON." },
          { role: "user", content: prompt },
        ],
        max_tokens: 300,
      }),
    });

    const data = await openaiRes.json() as any;
    const raw: string = data.choices?.[0]?.message?.content?.trim() ?? "";

    const jsonStart = raw.indexOf("{");
    const jsonEnd = raw.lastIndexOf("}");
    if (jsonStart === -1 || jsonEnd === -1) throw new Error("No JSON in response");

    const obj = JSON.parse(raw.slice(jsonStart, jsonEnd + 1));
    const options: string[] = Array.isArray(obj.options) ? obj.options.slice(0, 4) : [];

    res.json({
      question: obj.question ?? "",
      options,
      correct: typeof obj.correct === "number" ? Math.max(0, Math.min(3, obj.correct)) : 0,
      explanation: obj.explanation ?? "",
    });
  } catch (err) {
    res.status(500).json({ error: "Quiz generation failed" });
  }
});

// ─── POST /botanical-story ────────────────────────────────────────────────────
// Generates a 4-part rich botanical narrative via GPT-4o-mini server-side.
router.post("/botanical-story", async (req, res) => {
  const { plantName, scientificName } = req.body as {
    plantName?: string;
    scientificName?: string | null;
  };

  if (!plantName) {
    res.status(400).json({ error: "plantName is required" });
    return;
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    res.status(503).json({ error: "Story generation unavailable" });
    return;
  }

  const scientificPart = scientificName ? ` (${scientificName})` : "";
  const prompt = `Write a rich 4-part botanical story about "${plantName}"${scientificPart}.
Return ONLY valid JSON with no markdown, no explanation:
{
  "etymology": "2-3 sentences about the origin of the common and scientific name, its Latin/Greek roots, and who named it.",
  "history": "2-3 sentences about its medicinal uses, cultural significance, and historical mentions across civilizations.",
  "folklore": "2-3 sentences about myths, legends, symbolism, and traditional beliefs about this plant.",
  "ecology": "2-3 sentences about its natural habitat, ecological role, pollinators, and remarkable adaptations."
}`;

  try {
    const openaiRes = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        messages: [
          { role: "system", content: "You are a botanical storyteller. Return only valid JSON with no markdown." },
          { role: "user", content: prompt },
        ],
        max_tokens: 520,
      }),
    });

    const data = await openaiRes.json() as any;
    const raw: string = data.choices?.[0]?.message?.content?.trim() ?? "";
    const jsonStart = raw.indexOf("{");
    const jsonEnd = raw.lastIndexOf("}");
    if (jsonStart === -1 || jsonEnd === -1) throw new Error("No JSON in response");

    const obj = JSON.parse(raw.slice(jsonStart, jsonEnd + 1));
    res.json({
      etymology: obj.etymology ?? "",
      history: obj.history ?? "",
      folklore: obj.folklore ?? "",
      ecology: obj.ecology ?? "",
    });
  } catch (err) {
    res.status(500).json({ error: "Story generation failed" });
  }
});

// ─── POST /identify ───────────────────────────────────────────────────────────
// Proxies PlantNet identification server-side so the API key never leaves the server.
// Body: { imageBase64: string, lang?: string }
router.post("/identify", async (req, res) => {
  const { imageBase64, lang = "en" } = req.body as {
    imageBase64?: string;
    lang?: string;
  };

  if (!imageBase64) {
    res.status(400).json({ error: "imageBase64 is required" });
    return;
  }

  const apiKey = process.env.PLANTNET_API_KEY;
  if (!apiKey) {
    res.status(503).json({ error: "Plant identification unavailable — API key not configured" });
    return;
  }

  try {
    const imageBuffer = Buffer.from(imageBase64, "base64");
    const form = new FormData();
    const blob = new Blob([imageBuffer], { type: "image/jpeg" });
    form.append("images", blob, "plant.jpg");

    const plantNetRes = await fetch(
      `https://my-api.plantnet.org/v2/identify/all?api-key=${apiKey}&lang=${lang}&include-related-images=false`,
      { method: "POST", body: form }
    );

    if (!plantNetRes.ok) {
      const errText = await plantNetRes.text();
      res.status(plantNetRes.status).json({ error: `PlantNet error: ${errText}` });
      return;
    }

    const data = await plantNetRes.json();
    res.json(data);
  } catch (err) {
    res.status(500).json({ error: "Identification failed" });
  }
});

// ─── POST /plant-care ─────────────────────────────────────────────────────────
// Generates practical care tips via GPT-4o-mini for any identified plant.
router.post("/plant-care", async (req, res) => {
  const { plantName, scientificName } = req.body as {
    plantName?: string;
    scientificName?: string | null;
  };

  if (!plantName) {
    res.status(400).json({ error: "plantName is required" });
    return;
  }

  const apiKey = process.env.OPENAI_API_KEY;
  if (!apiKey) {
    res.status(503).json({ error: "Care tips unavailable" });
    return;
  }

  const sci = scientificName ? ` (${scientificName})` : "";
  const prompt = `Give concise practical care tips for "${plantName}"${sci}.
Return ONLY valid JSON with no markdown, no explanation:
{
  "watering": "One short sentence. E.g.: Every 7–10 days; let soil dry between waterings.",
  "light": "One short sentence. E.g.: Bright indirect light; avoid harsh direct sun.",
  "soil": "One short sentence. E.g.: Well-draining potting mix with perlite.",
  "temperature": "One short sentence. E.g.: Thrives between 18–27°C; protect from frost.",
  "toxicity": "One short sentence. Must specify pets (cats/dogs) AND humans. E.g.: Toxic to cats and dogs; mildly irritating to humans.",
  "seasonalTip": "One short actionable tip tied to current season or dormancy. E.g.: Reduce watering in winter when growth slows."
}`;

  try {
    const openaiRes = await fetch("https://api.openai.com/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${apiKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: "gpt-4o-mini",
        messages: [
          { role: "system", content: "You are an expert botanist and plant care specialist. Return only JSON." },
          { role: "user", content: prompt },
        ],
        max_tokens: 350,
        temperature: 0.3,
      }),
    });

    const data = await openaiRes.json() as any;
    const raw: string = data.choices?.[0]?.message?.content?.trim() ?? "";
    const jsonStart = raw.indexOf("{");
    const jsonEnd = raw.lastIndexOf("}");
    if (jsonStart === -1 || jsonEnd === -1) throw new Error("No JSON in response");
    const obj = JSON.parse(raw.slice(jsonStart, jsonEnd + 1));

    res.json({
      watering:    obj.watering    ?? "",
      light:       obj.light       ?? "",
      soil:        obj.soil        ?? "",
      temperature: obj.temperature ?? "",
      toxicity:    obj.toxicity    ?? "",
      seasonalTip: obj.seasonalTip ?? "",
    });
  } catch (err) {
    res.status(500).json({ error: "Care tips generation failed" });
  }
});

export default router;

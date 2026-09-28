import { createServer } from "node:http";
import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

async function loadLocalEnvironment() {
  const envPath = join(dirname(fileURLToPath(import.meta.url)), ".env");
  let contents;
  try {
    contents = await readFile(envPath, "utf8");
  } catch (error) {
    if (error.code === "ENOENT") return;
    throw error;
  }
  for (const line of contents.split(/\r?\n/)) {
    const entry = line.trim();
    if (!entry || entry.startsWith("#")) continue;
    const separator = entry.indexOf("=");
    if (separator < 1) continue;
    const name = entry.slice(0, separator).trim();
    let value = entry.slice(separator + 1).trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (/^[A-Za-z_][A-Za-z0-9_]*$/.test(name) && process.env[name] == null) {
      process.env[name] = value;
    }
  }
}

await loadLocalEnvironment();

const PORT = Number(process.env.PORT ?? 8080);
const GEMINI_MODEL = process.env.GEMINI_MODEL ?? "gemini-3.8-flash";
const GEMINI_API_KEY = process.env.GEMINI_API_KEY;
const MAX_IMAGE_BASE64_LENGTH = 6_000_000;

const responseSchema = {
  type: "OBJECT",
  properties: {
    isFood: { type: "BOOLEAN" },
    dishName: { type: "STRING" },
    description: { type: "STRING" },
    ingredients: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          name: { type: "STRING" },
          evidence: { type: "STRING", enum: ["VISIBLE", "TYPICAL"] },
        },
        required: ["name", "evidence"],
        propertyOrdering: ["name", "evidence"],
      },
    },
    uncertainty: { type: "STRING" },
  },
  required: ["isFood", "dishName", "description", "ingredients", "uncertainty"],
  propertyOrdering: ["isFood", "dishName", "description", "ingredients", "uncertainty"],
};

const recommendationSchema = {
  type: "OBJECT",
  properties: {
    recommendations: {
      type: "ARRAY",
      items: {
        type: "OBJECT",
        properties: {
          type: { type: "STRING", enum: ["DISH", "RESTAURANT"] },
          name: { type: "STRING" },
          reason: { type: "STRING" },
        },
        required: ["type", "name", "reason"],
        propertyOrdering: ["type", "name", "reason"],
      },
    },
  },
  required: ["recommendations"],
  propertyOrdering: ["recommendations"],
};

function sendJson(response, status, body) {
  response.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
  });
  response.end(JSON.stringify(body));
}

async function readJson(request) {
  const chunks = [];
  let size = 0;
  for await (const chunk of request) {
    size += chunk.length;
    if (size > 8_000_000) throw Object.assign(new Error("Request too large"), { status: 413 });
    chunks.push(chunk);
  }
  try {
    return JSON.parse(Buffer.concat(chunks).toString("utf8"));
  } catch {
    throw Object.assign(new Error("Invalid JSON"), { status: 400 });
  }
}

function validImage({ mimeType, imageBase64 }) {
  return ["image/jpeg", "image/png", "image/webp"].includes(mimeType)
    && typeof imageBase64 === "string"
    && imageBase64.length > 0
    && imageBase64.length <= MAX_IMAGE_BASE64_LENGTH
    && /^[A-Za-z0-9+/]+={0,2}$/.test(imageBase64);
}

const server = createServer(async (request, response) => {
  const url = new URL(request.url ?? "/", `http://${request.headers.host ?? "localhost"}`);

  if (request.method === "GET" && url.pathname === "/health") {
    return sendJson(response, 200, { status: "ok", aiConfigured: Boolean(GEMINI_API_KEY) });
  }

  if (request.method === "POST" && url.pathname === "/recommendations") {
    if (!GEMINI_API_KEY) return sendJson(response, 503, { error: "Configure GEMINI_API_KEY on the server." });
    try {
      const input = await readJson(request);
      const validItems = (items) => Array.isArray(items)
        && items.length <= 20
        && items.every((item) => typeof item?.name === "string" && item.name.trim().length > 0
          && item.name.length <= 120 && Number.isFinite(item.rating) && item.rating >= 0 && item.rating <= 5);
      if (!validItems(input.favoriteDishes) || !validItems(input.placesToRepeat)) {
        return sendJson(response, 400, { error: "Invalid taste profile." });
      }

      const tasteEvidence = {
        favoriteDishes: input.favoriteDishes.map(({ name, rating }) => ({ name: name.trim(), rating })),
        placesToRepeat: input.placesToRepeat.map(({ name, rating }) => ({ name: name.trim(), rating })),
      };
      const prompt = `Eres un asistente gastronómico para una memoria personal. Responde en español y solo con el JSON solicitado. `
        + `Usa únicamente este resumen real del historial: ${JSON.stringify(tasteEvidence)}. `
        + `Propón hasta 3 platos que tengan sentido por similitud con los favoritos; preséntalos como ideas, sin afirmar que el usuario ya los probó. `
        + `Sugiere restaurantes únicamente de la lista placesToRepeat, sin inventar locales, ubicaciones ni datos externos. `
        + `Cada motivo debe explicar la relación concreta con las valoraciones. Si no hay evidencia suficiente para un tipo, no lo incluyas. `
        + `No recibes ni necesitas notas, acompañantes, fotografías ni información personal.`;
      const upstream = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(GEMINI_MODEL)}:generateContent`,
        {
          method: "POST",
          headers: { "content-type": "application/json", "x-goog-api-key": GEMINI_API_KEY },
          body: JSON.stringify({
            contents: [{ role: "user", parts: [{ text: prompt }] }],
            generationConfig: { responseMimeType: "application/json", responseSchema: recommendationSchema },
          }),
          signal: AbortSignal.timeout(45_000),
        },
      );
      if (!upstream.ok) {
        console.error("Gemini recommendations failed with HTTP", upstream.status);
        return sendJson(response, 502, { error: "The recommendations service could not process this profile." });
      }
      const result = await upstream.json();
      const generatedText = result.candidates?.[0]?.content?.parts?.find((part) => typeof part.text === "string")?.text;
      if (!generatedText) return sendJson(response, 502, { error: "The AI returned no recommendations." });
      let parsed;
      try { parsed = JSON.parse(generatedText); } catch {
        return sendJson(response, 502, { error: "The AI returned unreadable recommendations." });
      }
      const allowedRestaurants = new Set(tasteEvidence.placesToRepeat.map((place) => place.name.toLocaleLowerCase("es")));
      const recommendations = Array.isArray(parsed.recommendations)
        ? parsed.recommendations.flatMap((item) => {
          const type = item?.type === "RESTAURANT" ? "RESTAURANT" : item?.type === "DISH" ? "DISH" : null;
          const name = typeof item?.name === "string" ? item.name.trim().slice(0, 120) : "";
          const reason = typeof item?.reason === "string" ? item.reason.trim().slice(0, 240) : "";
          if (!type || !name || !reason) return [];
          if (type === "RESTAURANT" && !allowedRestaurants.has(name.toLocaleLowerCase("es"))) return [];
          return [{ type, name, reason }];
        }).slice(0, 6)
        : [];
      return sendJson(response, 200, { recommendations });
    } catch (error) {
      if (error?.name === "TimeoutError") return sendJson(response, 504, { error: "The recommendations request timed out." });
      return sendJson(response, error.status ?? 500, { error: error.status ? error.message : "The recommendations service failed." });
    }
  }

  if (request.method !== "POST" || url.pathname !== "/analyze-dish") {
    return sendJson(response, 404, { error: "Not found" });
  }
  if (!GEMINI_API_KEY) {
    return sendJson(response, 503, { error: "Configure GEMINI_API_KEY on the server." });
  }

  try {
    const input = await readJson(request);
    if (!validImage(input)) return sendJson(response, 400, { error: "Unsupported or invalid image." });

    const prompt = `Analiza esta foto de un plato para una memoria gastronómica personal. Responde en español y en el JSON solicitado.\n\n` +
      `Identifica el plato solo hasta donde permita la imagen. Describe brevemente lo que se ve. ` +
      `En ingredients incluye solo ingredientes relevantes: usa evidence=VISIBLE cuando haya evidencia visual directa y evidence=TYPICAL cuando sea habitual del plato pero no se pueda confirmar en la foto. ` +
      `No afirmes alérgenos ni ingredientes ocultos como hechos. Si no puedes reconocer algo con suficiente seguridad, deja dishName vacío o explícalo en uncertainty. ` +
      `Si la imagen no es comida, usa isFood=false y deja dishName vacío. No inventes cantidades, receta ni procedencia.`;

    const upstream = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(GEMINI_MODEL)}:generateContent`,
      {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-goog-api-key": GEMINI_API_KEY,
        },
        body: JSON.stringify({
          contents: [{
            role: "user",
            parts: [
              { inline_data: { mime_type: input.mimeType, data: input.imageBase64 } },
              { text: prompt },
            ],
          }],
          generationConfig: {
            responseMimeType: "application/json",
            responseSchema,
          },
        }),
        signal: AbortSignal.timeout(45_000),
      },
    );

    if (!upstream.ok) {
      console.error("Gemini request failed with HTTP", upstream.status);
      return sendJson(response, 502, { error: "The dish analysis service could not process this photo." });
    }

    const result = await upstream.json();
    const generatedText = result.candidates?.[0]?.content?.parts
      ?.find((part) => typeof part.text === "string")?.text;
    if (!generatedText) return sendJson(response, 502, { error: "The AI returned no analysis." });

    let analysis;
    try {
      analysis = JSON.parse(generatedText);
    } catch {
      return sendJson(response, 502, { error: "The AI returned an unreadable analysis." });
    }

    const ingredients = Array.isArray(analysis.ingredients)
      ? analysis.ingredients
          .filter((item) => typeof item?.name === "string" && item.name.trim())
          .map((item) => ({
            name: item.name.trim().slice(0, 80),
            evidence: item.evidence === "VISIBLE" ? "VISIBLE" : "TYPICAL",
          }))
          .slice(0, 20)
      : [];

    return sendJson(response, 200, {
      isFood: analysis.isFood === true,
      dishName: typeof analysis.dishName === "string" ? analysis.dishName.trim().slice(0, 120) : "",
      description: typeof analysis.description === "string" ? analysis.description.trim().slice(0, 400) : "",
      ingredients,
      uncertainty: typeof analysis.uncertainty === "string" ? analysis.uncertainty.trim().slice(0, 240) : "",
    });
  } catch (error) {
    if (error?.name === "TimeoutError") return sendJson(response, 504, { error: "The dish analysis timed out." });
    return sendJson(response, error.status ?? 500, {
      error: error.status ? error.message : "The dish analysis service failed.",
    });
  }
});

server.listen(PORT, "0.0.0.0", () => {
  console.log(`Food Memory AI demo service listening on http://0.0.0.0:${PORT}`);
  console.log(`Gemini model: ${GEMINI_MODEL}; API key configured: ${Boolean(GEMINI_API_KEY)}`);
});

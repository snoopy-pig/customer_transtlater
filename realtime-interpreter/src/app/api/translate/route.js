import { NextResponse } from 'next/server';
import { GoogleGenerativeAI } from '@google/generative-ai';

const langNames = {
  'ko-KR': 'Korean',
  'en-US': 'English',
  'ja-JP': 'Japanese',
  'zh-CN': 'Chinese Simplified',
  'zh-TW': 'Chinese Traditional',
  'es-ES': 'Spanish',
  'fr-FR': 'French',
  'de-DE': 'German',
  'vi-VN': 'Vietnamese',
  'th-TH': 'Thai',
  'id-ID': 'Indonesian',
  'ru-RU': 'Russian',
  'it-IT': 'Italian',
  'pt-PT': 'Portuguese'
};

export async function POST(request) {
  let text = '';
  try {
    const body = await request.json();
    text = body.text;
    const { sourceLang, targetLang, glossary } = body;

    if (!text || !text.trim()) {
      return NextResponse.json({ translation: '' });
    }

    const sourceLangName = langNames[sourceLang] || sourceLang;
    const targetLangName = langNames[targetLang] || targetLang;

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey || apiKey === 'YOUR_GEMINI_API_KEY_HERE') {
      console.warn("GEMINI_API_KEY is not configured or is placeholder. Streaming mock translation.");
      const mockText = `[Mock Translation (${sourceLangName} -> ${targetLangName})]: ${text}`;
      const encoder = new TextEncoder();
      const stream = new ReadableStream({
        async start(controller) {
          const chunks = [];
          // Break into chunks of 2-3 characters to simulate typing effect
          for (let i = 0; i < mockText.length; i += 3) {
            chunks.push(mockText.slice(i, i + 3));
          }
          for (const chunk of chunks) {
            controller.enqueue(encoder.encode(chunk));
            await new Promise(resolve => setTimeout(resolve, 50)); // 50ms delay
          }
          controller.close();
        }
      });
      return new Response(stream, {
        headers: { 'Content-Type': 'text/plain; charset=utf-8' }
      });
    }

    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ 
      model: 'gemini-3.5-flash-lite',
      generationConfig: { 
        responseMimeType: "text/plain",
        temperature: 0.1,
        maxOutputTokens: 150
      }
    });

    let glossaryInstructions = '';
    if (glossary && glossary.length > 0) {
      glossaryInstructions = `Glossary (Strictly translate these terms as specified if they appear in text):
${glossary.map(g => `- "${g.term}" -> "${g.translation}"`).join('\n')}`;
    }

    const prompt = `Translate the following speech transcript from ${sourceLangName} to ${targetLangName}.
Output ONLY the translation. Do not include any quotes, markdown, explanations, or additional text.

${glossaryInstructions}

Text to translate:
"${text}"`;

    const result = await model.generateContentStream(prompt);
    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      async start(controller) {
        try {
          for await (const chunk of result.stream) {
            const chunkText = chunk.text();
            controller.enqueue(encoder.encode(chunkText));
          }
        } catch (err) {
          console.error("Stream generation error:", err);
          controller.enqueue(encoder.encode(`\n[번역 오류: ${err.message}]`));
        } finally {
          controller.close();
        }
      }
    });

    return new Response(stream, {
      headers: { 'Content-Type': 'text/plain; charset=utf-8' }
    });
  } catch (error) {
    console.error('Translation error:', error);
    // Return a readable error stream instead of 500 status to prevent UI crash
    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode(`[번역 실패 - 원문]: ${text}`));
        controller.close();
      }
    });
    return new Response(stream, {
      headers: { 'Content-Type': 'text/plain; charset=utf-8' }
    });
  }
}

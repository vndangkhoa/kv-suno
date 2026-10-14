/**
 * Evaluates a Suno AI prompt against the 5 Pillars:
 * 1. Genre (Specific subgenres, era, fusion)
 * 2. Mood & Tempo (Emotional tone, energy, BPM)
 * 3. Instruments (Acoustic, electronic, rhythm section, lead elements)
 * 4. Vocals (Vocal style, gender, delivery, harmony)
 * 5. Mixing & Production (Effects, master quality, soundstage, reverb)
 */
export function scoreAndOptimizePrompt(promptText = '', language = 'vi') {
  const p = promptText.trim().toLowerCase();

  let score = 40;
  const strengths = [];
  const warnings = [];
  const suggestedTags = [];

  // Pillar 1: Genre Detection
  let detectedGenre = "General / Pop Fusion";
  const genres = [
    { name: "Vietnamese Folk Trap / V-Pop", keywords: ["vietnam", "folk", "trap", "v-pop", "dân gian", "vọng cổ"] },
    { name: "Cinematic Epic / Orchestral", keywords: ["cinematic", "epic", "orchestra", "trailer", "soundtrack", "symphonic"] },
    { name: "Electronic Dance Music / EDM", keywords: ["edm", "future bass", "synthwave", "house", "trance", "techno", "dubstep"] },
    { name: "Hip-Hop / Urban Rap", keywords: ["hip hop", "rap", "boom bap", "drill", "lo-fi", "lofi"] },
    { name: "Rock / Metal / Alternative", keywords: ["rock", "metal", "grunge", "punk", "indie rock"] },
    { name: "Acoustic Ballad / R&B", keywords: ["ballad", "acoustic", "r&b", "soul", "piano ballad"] }
  ];

  for (const g of genres) {
    if (g.keywords.some(k => p.includes(k))) {
      detectedGenre = g.name;
      score += 15;
      strengths.push(language === 'vi' ? `Thể loại rõ ràng: ${g.name}` : `Well-defined genre: ${g.name}`);
      break;
    }
  }

  // Pillar 2: Mood & Energy
  let detectedMood = "Emotional & Catchy";
  const moods = [
    { name: "Eerie, Dark, Haunting", keywords: ["dark", "eerie", "haunting", "u ám", "ma mị", "creepy"] },
    { name: "Euphoric, High Energy, Uplifting", keywords: ["energetic", "euphoric", "uplifting", "sôi động", "vui tươi", "bùng nổ"] },
    { name: "Melancholic, Nostalgic, Sad", keywords: ["sad", "melancholy", "nostalgic", "buồn", "sầu", "hoài niệm"] },
    { name: "Chill, Atmospheric, Ambient", keywords: ["chill", "ambient", "atmospheric", "thư giãn", "êm dịu"] }
  ];

  for (const m of moods) {
    if (m.keywords.some(k => p.includes(k))) {
      detectedMood = m.name;
      score += 12;
      strengths.push(language === 'vi' ? `Tâm trạng xác định: ${m.name}` : `Expressive mood: ${m.name}`);
      break;
    }
  }

  // Pillar 3: Instruments
  let detectedInst = "Standard rhythm section";
  const instruments = [
    { name: "808 Sub Bass, Dan Tranh, Bamboo Flute", keywords: ["808", "tranh", "sáo", "flute", "bass"] },
    { name: "Electric Guitar, Distorted Riffs, Drums", keywords: ["guitar", "distort", "drum", "drums"] },
    { name: "Acoustic Piano, Warm Strings, Cello", keywords: ["piano", "strings", "cello", "violin"] },
    { name: "Synth Plucks, Rolling Arpeggiator, Sub Bass", keywords: ["synth", "arp", "sub bass", "lead synth"] }
  ];

  for (const inst of instruments) {
    if (inst.keywords.some(k => p.includes(k))) {
      detectedInst = inst.name;
      score += 15;
      strengths.push(language === 'vi' ? `Nhạc cụ phong phú: ${inst.name}` : `Specific instrumentation: ${inst.name}`);
      break;
    }
  }

  // Pillar 4: Vocals
  let detectedVocals = "Clear expressive vocal delivery";
  const vocals = [
    { name: "Fast aggressive male rap flow", keywords: ["rap", "male", "aggressive", "flow"] },
    { name: "Female airy emotive lead vocals", keywords: ["female", "airy", "emotive", "nữ"] },
    { name: "Layered choral harmonies & whispers", keywords: ["whisper", "harmony", "choir", "vocal chop"] },
    { name: "Instrumental (No vocals)", keywords: ["instrumental", "không lời"] }
  ];

  for (const v of vocals) {
    if (v.keywords.some(k => p.includes(k))) {
      detectedVocals = v.name;
      score += 10;
      strengths.push(language === 'vi' ? `Phong cách giọng hát cụ thể` : `Clear vocal guidance`);
      break;
    }
  }

  // Pillar 5: Mixing & Studio Quality
  let detectedMixing = "Clean modern studio mix, spatial reverb";
  const mixingTags = ["reverb", "delay", "saturated", "mastered", "spatial", "hi-fi", "cinematic riser", "beat drop"];
  const hasMixing = mixingTags.filter(t => p.includes(t));
  if (hasMixing.length > 0) {
    detectedMixing = `${hasMixing.join(", ")} production mix`;
    score += 8;
    strengths.push(language === 'vi' ? `Hiệu ứng mixing chuyên nghiệp` : `Studio mixing descriptors`);
  } else {
    warnings.push(language === 'vi' ? `Chưa có thông số mixing (reverb, bass drop, saturation)` : `Missing mixing/production tags`);
    suggestedTags.push("spatial reverb", "hard 808 drop", "wide stereo mix");
  }

  // Length and coherence checks
  if (p.length < 20) {
    warnings.push(language === 'vi' ? `Prompt quá ngắn, Suno dễ tạo nhạc chung chung` : `Prompt is too short, may result in generic music`);
    score = Math.max(score - 15, 35);
  } else if (p.length > 300) {
    warnings.push(language === 'vi' ? `Prompt quá dài có thể bị Suno cắt bớt các tag cuối` : `Prompt is lengthy; Suno may truncate trailing tags`);
  }

  // Normalize score
  score = Math.min(Math.max(score, 45), 98);

  let producerGrade = "B";
  if (score >= 90) producerGrade = "A+";
  else if (score >= 80) producerGrade = "A";
  else if (score >= 65) producerGrade = "B";
  else producerGrade = "C";

  // Build optimized prompt
  const tagList = [
    detectedGenre !== "General / Pop Fusion" ? detectedGenre.toLowerCase().replace(/\s*\/\s*/g, ", ") : "modern fusion",
    detectedMood.toLowerCase(),
    detectedInst.toLowerCase(),
    detectedVocals.toLowerCase(),
    "professional studio mastering, crisp audio mix"
  ];

  const optimizedPrompt = `${promptText ? promptText + ', ' : ''}${tagList.join(", ")}`
    .replace(/,\s*,/g, ',')
    .trim();

  return {
    score,
    producerGrade,
    strengths: strengths.length > 0 ? strengths : [language === 'vi' ? "Ý tưởng cơ bản khả thi" : "Baseline musical concept"],
    warnings: warnings.length > 0 ? warnings : [language === 'vi' ? "Prompt đạt chuẩn tốt" : "Prompt follows production best practices"],
    suggestedTags: suggestedTags.length > 0 ? suggestedTags : ["driving tempo", "crisp drums", "wide reverb", "dynamic drop"],
    pillars: {
      genre: detectedGenre,
      mood: detectedMood,
      instruments: detectedInst,
      vocals: detectedVocals,
      mixing: detectedMixing
    },
    optimizedPrompt
  };
}

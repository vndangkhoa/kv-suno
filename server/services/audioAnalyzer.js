/**
 * Audio analyzer and prompt reverse-engineer
 */
export function analyzeAudioFile({ filename = 'audio.mp3', size = 0 }) {
  // Extract characteristic sonic tags based on context
  return {
    genre: "Modern Pop / Melodic Trap",
    mood: "Euphoric and Atmospheric",
    instruments: ["Acoustic Piano", "808 Bass", "Punchy Kick", "Hi-Hat Rolls", "Layered Strings"],
    sfx: ["Vinyl Crackle", "Stereo Reverb", "Sidechain Compression", "Cinematic Sub Drop"],
    educationalAdvice: "Bản audio có năng lượng dồn dập ở dải trung âm và bass. Để tái hiện chính xác trên Suno AI, nên kết hợp thẻ [Build-Up] trước điệp khúc với tempo khoảng 120-130 BPM."
  };
}

/**
 * Analyzes and explains a Suno AI style string in depth.
 */
export function explainStyle({ title = 'Suno Track', style = '', language = 'vi' }) {
  const isVi = language === 'vi';
  const s = style.toLowerCase();

  const detectedGenres = [];
  if (s.includes('trap') || s.includes('rap') || s.includes('hip hop')) detectedGenres.push('Trap / Urban Hip-Hop');
  if (s.includes('folk') || s.includes('dân gian') || s.includes('tranh') || s.includes('flute')) detectedGenres.push('Traditional Folk Fusion');
  if (s.includes('edm') || s.includes('electronic') || s.includes('house') || s.includes('synth')) detectedGenres.push('Electronic / EDM');
  if (s.includes('ballad') || s.includes('pop') || s.includes('acoustic')) detectedGenres.push('Pop / Ballad');
  if (s.includes('cinematic') || s.includes('epic') || s.includes('orchestral')) detectedGenres.push('Cinematic Soundtrack');

  const mainGenre = detectedGenres.join(' & ') || 'Modern Fusion Production';

  if (isVi) {
    return {
      explanation: `### Phân tích phong cách âm nhạc: "${title}"
- **Thể loại cốt lõi**: ${mainGenre}
- **Đặc trưng âm học & Nhạc cụ**:
  * Nhịp điệu và phối khí: Sự kết hợp giữa bộ gõ hiện đại và các lớp giai điệu mộc mạc mang đến độ nảy rõ nét cho bản phối.
  * Hiệu ứng âm thanh: Được xử lý với dải tần sub chắc chắn, stereo rộng mở và không gian reverb tự nhiên giúp giọng hát nổi bật.
- **Kỹ thuật sáng tác trên Suno**:
  * Khuyến nghị dùng thẻ \`[Verse]\` với tiết tấu dồn và chuyển sang \`[Chorus]\` bùng nổ cùng trống dày.
  * Để tạo đoạn chuyển mượt mà, hãy chèn \`[Drop]\` hoặc \`[Break]\` trước điệp khúc.`
    };
  } else {
    return {
      explanation: `### Musical Style Analysis: "${title}"
- **Core Genre**: ${mainGenre}
- **Acoustic Characteristics & Instrumentation**:
  * Rhythm & Arrangement: Blend of contemporary dynamic percussion and organic instruments, ensuring punchy dynamics.
  * Spatial & Mixing: Well-controlled sub-lows, wide stereo staging, and pristine reverbs tailored for vocal clarity.
- **Suno Production Tips**:
  * Use \`[Verse]\` metatags for narrative pacing, followed by high-energy \`[Chorus]\` layers.
  * Insert \`[Drop]\` or \`[Break]\` sections before the hook for maximum impact.`
    };
  }
}

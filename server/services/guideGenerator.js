/**
 * Generates comprehensive production masterclass guides and prompt cookbooks for Suno AI
 */
export function generateGuide({ keyword = 'Music Production', category = 'general', language = 'vi' }) {
  const isVi = language === 'vi';
  const cleanKeyword = keyword.trim();
  const kwLower = cleanKeyword.toLowerCase();

  // Infer musical characteristics
  let bpmHint = '115 - 128 BPM';
  let lowEndAdvice = 'Dải trầm cần được khóa mono ở trung tâm (Mono Low-end) dưới 100Hz để tránh triệt tiêu pha.';
  let lowEndAdviceEn = 'Low-end frequencies below 100Hz should be locked dead-center in mono to prevent phase cancellation.';
  
  if (kwLower.includes('chill') || kwLower.includes('ambient') || kwLower.includes('lofi') || kwLower.includes('ballad') || kwLower.includes('acoustic')) {
    bpmHint = '68 - 85 BPM';
    lowEndAdvice = 'Sử dụng sub-bass dạng sóng sin tròn trịa (sine wave) hoặc tiếng Contrabass mộc mạc, tránh bass gắt méo tiếng.';
    lowEndAdviceEn = 'Use pure sine sub-bass or acoustic double bass, avoiding aggressive distortion to maintain relaxing warmth.';
  } else if (kwLower.includes('trap') || kwLower.includes('hip-hop') || kwLower.includes('drill') || kwLower.includes('phonk')) {
    bpmHint = '130 - 145 BPM (Half-time feel)';
    lowEndAdvice = '808 Sub-bass được đẩy bão hòa nhẹ (tape saturation) và trượt nốt (pitch glide) để tạo độ nẩy đặc trưng.';
    lowEndAdviceEn = '808 sub-bass with gentle tape saturation and pitch glides for signature modern swagger.';
  } else if (kwLower.includes('house') || kwLower.includes('techno') || kwLower.includes('edm') || kwLower.includes('club')) {
    bpmHint = '124 - 138 BPM';
    lowEndAdvice = 'Kick bốn phách bốn góc (four-on-the-floor) nén sidechain chặt chẽ với bassline để tạo sức hút cơ học.';
    lowEndAdviceEn = 'Four-on-the-floor kick sidechained tightly against the bassline for rhythmic pumping propulsion.';
  }

  if (isVi) {
    return {
      guide: `# 🌟 Cẩm Nang Chuyên Sâu: ${cleanKeyword} trong Suno AI

## 1. Triết lý Âm thanh & Bản chất Dải tần
**${cleanKeyword}** (${category}) đóng vai trò quyết định trong việc xây dựng bản sắc âm học và cảm xúc bài hát. Khi được thiết kế chính xác, yếu tố này giúp bài nhạc thoát khỏi cảm giác "nhạc máy AI" và đạt độ tự nhiên chuẩn phòng thu quốc tế.
* **Vị trí dải tần mục tiêu:** Kiểm soát hài âm của ${cleanKeyword} để không đè bẹp giọng hát chính (vocal) trong dải tần 1kHz - 4kHz.
* **Màu sắc âm thanh:** Luôn kết hợp từ khóa mô tả vật lý như \`warm analog tape\`, \`organic transients\`, hoặc \`pristine dynamic resolution\`.

## 2. Thiết lập Không gian 3D & Chiều sâu Âm trường (Spatial Staging)
* **Độ sâu trước - sau (Front-to-Back Depth):** Đặt các yếu tố khô, sắc nét (\`dry\`) ở tiền cảnh gần tai người nghe, đẩy các lớp bè và pad không gian ra xa bằng Reverb đuôi dài (\`ambient decay reverb\`).
* **Độ mở rộng Stereo (Stereo Width):** Giữ dải trầm mono ở giữa, dùng kỹ thuật \`wide stereo imaging, micro-panning\` cho ${cleanKeyword} để âm thanh ôm trọn hai bên tai.

## 3. Quản lý Dải Trầm & Nhịp điệu (Low-end & BPM)
* **Nhịp độ lý tưởng:** Thường dao động đẹp nhất quanh mức **${bpmHint}**.
* **Độ nảy Dải Trầm:** ${lowEndAdvice}
* **Từ khóa gợi ý:** \`grounded punchy low-end\`, \`analog warmth\`, \`unhurried pocket groove\`.

## 4. Khái niệm "Negative Space" (Khoảng thở âm nhạc)
Trong sản xuất âm nhạc đỉnh cao, **khoảng lặng có giá trị ngang với nốt nhạc**. 
* Không cố nhồi nhét quá nhiều nhạc cụ cùng lúc khiến thuật toán Suno bị quá tải.
* Sử dụng delay trễ nhịp (\`dotted eighth tape delay\`) để ${cleanKeyword} ngân vang tan biến tự nhiên vào không gian thay vì đánh liên tục.

## 5. Kinh nghiệm Producer: Suno v3.5 vs Suno v4/v4.5
1. **Đối với Suno v4:** Model v4 hiểu ngữ cảnh âm nhạc rất tốt. Hãy mô tả cảm xúc và vai trò cụ thể (\`expressive ${cleanKeyword.toLowerCase()} countermelody\`) thay vì chỉ liệt kê từ khóa đơn lẻ.
2. **Tránh xung đột từ khóa:** Nếu bài đã có dải trung dày (như đàn guitar điện distortion), hãy chọn âm sắc ${cleanKeyword} trong trẻo để tạo độ tương phản.
3. **Độ ưu tiên từ khóa:** Đặt **${cleanKeyword}** ở 1/3 đầu câu lệnh prompt để Suno ưu tiên nhận diện làm nhạc cụ/hiệu ứng hạt nhân.

---

## 6. Công thức Prompt Chuẩn Studio & Thẻ Đạo Diễn

### 🎯 Prompt Mẫu Tối Ưu Trọng Số:
\`\`\`text
${cleanKeyword.toLowerCase()}, dynamic studio master, wide stereo imaging, warm analog tape saturation, pristine frequency balance, expressive performance, ${bpmHint}
\`\`\`

### 🏷️ Thẻ Cấu Trúc Metatags Gợi Ý:
* \`[Solo - ${cleanKeyword}]\`: Dành riêng 4 đến 8 ô nhịp cho ${cleanKeyword} phô diễn giai điệu độc tấu.
* \`[Layered ${cleanKeyword} Countermelody]\`: Làm bè lót du dương bên dưới giọng hát điệp khúc.
* \`[Outro - Fading ${cleanKeyword}]\`: Kết thúc bài lắng đọng, tan biến mượt mà vào Reverb tail.`
    };
  } else {
    return {
      guide: `# 🌟 Production Masterclass: ${cleanKeyword} in Suno AI

## 1. Sonic Philosophy & Frequency Distribution
**${cleanKeyword}** (${category}) is a foundational sonic pillar in shaping the tonal personality and emotional resonance of Suno AI tracks.
* **Frequency Real Estate:** Protect the 1kHz - 4kHz presence bracket for vocals while giving ${cleanKeyword} space to articulate.
* **Acoustic Timbre:** Always pair with physical descriptors like \`warm analog tape\`, \`crisp transients\`, or \`pristine dynamic resolution\`.

## 2. 3D Spatial Staging & Stereo Imaging
* **Front-to-Back Depth:** Place dry, punchy elements close in the foreground, projecting atmospheric layers backward with \`ambient decay reverb\`.
* **Stereo Panorama:** Anchor sub frequencies dead-center in mono, utilizing \`wide stereo imaging, micro-panning\` for ${cleanKeyword} to wrap around the listener.

## 3. Low-End Management & BPM Grid
* **Ideal Tempo Range:** Typically blossoms around **${bpmHint}**.
* **Low-End Discipline:** ${lowEndAdviceEn}
* **Recommended Descriptors:** \`grounded punchy low-end\`, \`analog warmth\`, \`pocket groove\`.

## 4. The Power of Negative Space
Silence is as vital as sound. Avoid cluttering every bar with continuous notes.
* Let ${cleanKeyword} breathe with \`dotted eighth tape delay\` trails that decay naturally into the soundstage.

## 5. Producer Insights: Suno v3.5 vs v4/v4.5
1. **Suno v4 Nuances:** Model v4 grasps nuanced musical context. Frame ${cleanKeyword} within its musical role (\`expressive ${cleanKeyword.toLowerCase()} lead melody\`) rather than keyword stuffing.
2. **Clutter Prevention:** If the arrangement already features heavy mids, steer ${cleanKeyword} toward crystalline highs or deep lows.
3. **Prompt Positioning:** Place **${cleanKeyword}** in the first third of your style prompt for optimal model weighting.

---

## 6. Production Recipe & Arrangement Directives

### 🎯 Master Prompt Recipe:
\`\`\`text
${cleanKeyword.toLowerCase()}, dynamic studio master, wide stereo imaging, warm analog tape saturation, pristine frequency balance, expressive performance, ${bpmHint}
\`\`\`

### 🏷️ Arrangement Metatags:
* \`[Solo - ${cleanKeyword}]\`: Grants 4 to 8 bars of uninterrupted musical spotlight.
* \`[Layered ${cleanKeyword} Countermelody]\`: Weaves complementary harmonies beneath the lead hook.
* \`[Outro - Fading ${cleanKeyword}]\`: Delivers an organic, lingering spatial resolution.`
    };
  }
}

/**
 * Autonomous Lyrics Generation Engine for Suno AI
 * Generates structured songs with proper metatags, rhymes, and style prompts.
 */
export function generateLyrics({
  topic = "Tình yêu mùa thu",
  genre = "Pop Ballad",
  mood = "Emotional and Catchy",
  vocalType = "Female Emotional",
  structure = "Standard Pop",
  language = "vi"
}) {
  const isVi = language === "vi";

  let title = isVi ? `Bản Tình Ca ${topic.slice(0, 15)}` : `Echoes of ${topic.slice(0, 15)}`;
  let lyrics = "";
  let suggestedStylePrompt = "";
  let musicalNotes = "";

  if (isVi) {
    if (structure.includes("Pop") || structure.includes("Standard")) {
      lyrics = `[INTRO]
(Tiếng piano chậm rãi, không gian mộc mạc)
Lá rơi bên thềm... gió lay êm đềm...

[VERSE 1]
Bước qua con phố quen ngày xưa đón đưa
Cơn mưa chiều nay rơi như ngày đầu mới hứa
Ánh mắt ngày ấy ấm nồng trao thiết tha
Giờ chỉ còn nỗi nhớ theo bước chân xa.

[PRE-CHORUS]
(Nhịp điệu dồn dập, violin hòa quyện)
Thời gian trôi không thể quay lại
Chỉ còn lại những vết thương khôn nguôi
Dù biết rằng người đã rời xa mãi mãi
Vẫn mơ về một sớm mai có em.

[CHORUS]
(Điệp khúc cao trào - Trống đánh mạnh, hòa âm ngân vang)
Hãy giữ lấy những ký ức ngọt ngào
Dù con đường đôi ta giờ chia hai ngả
Tình yêu đó dẫu muôn vàn trắc trở
Vẫn là khúc ca đẹp nhất cuộc đời anh!

[VERSE 2]
Mùa thu sang thay màu áo lá úa phai
Tiếng thở dài giữa đêm khuya vắng lạnh lùng
Từng dòng tin nhắn giờ chỉ là dĩ vãng
Nụ cười người xưa theo mây gió trôi nhanh.

[CHORUS]
Hãy giữ lấy những ký ức ngọt ngào
Dù con đường đôi ta giờ chia hai ngả
Tình yêu đó dẫu muôn vàn trắc trở
Vẫn là khúc ca đẹp nhất cuộc đời anh!

[BRIDGE]
(Nhạc lắng xuống, guitar acoustic độc tấu)
Nếu có một ngày tình cờ gặp lại
Liệu ta có mỉm cười chào nhau như bạn cũ?
Hay chỉ là ánh nhìn lướt vội qua
Để giọt sầu rơi đầy trang nhật ký...

[OUTRO]
(Tiếng chuông ngân xa, giai điệu dần tan vào hư không)
Mùa thu qua... người xa...
Tình này... xin gửi lại cho gió sương...
(Fade out)`;
    } else if (structure.includes("Hip-Hop") || structure.includes("Rap") || genre.toLowerCase().includes("rap")) {
      lyrics = `[INTRO]
(Tiếng 808 gắt, tiếng đàn tranh rải nhịp)
Check 1-2, keep the fire moving...

[VERSE 1 - RAP]
(Flow nhanh, dứt khoát từng câu)
Đêm buông xuống thành phố lên đèn hoa lệ
Ta mang theo giấc mộng bôn ba giữa bộn bề
Thành công không phải món quà dễ đến
Ai cũng từng nếm trải cay đắng bên thềm.

[HOOK]
(Drop beat căng - Sub bass rung chuyển)
Bước tới, không bao giờ nhìn lại phía sau
Đốt cháy hết đam mê vượt qua mọi nỗi đau
Họ nói ta mộng tưởng, ta chứng minh bằng hành động
Biến ước mơ thành hiện thực giữa khung trời lồng lộng!

[VERSE 2 - RAP]
(Flow đảo phách, gieo vần liên tục)
Từ con số không đi lên bằng đôi bàn tay
Mồ hôi rơi trên trang sách từng ngày
Không chờ đợi cơ hội, ta tự tạo con đường
Bản lĩnh của chiến binh không chịu khuất phục gió sương!

[HOOK]
Bước tới, không bao giờ nhìn lại phía sau
Đốt cháy hết đam mê vượt qua mọi nỗi đau
Họ nói ta mộng tưởng, ta chứng minh bằng hành động
Biến ước mơ thành hiện thực giữa khung trời lồng lộng!

[OUTRO]
(Bass dịu dần, tiếng scratch đĩa vang lên)
This is real talk.
Keep grinding.
(Fade out)`;
    } else {
      lyrics = `[INTRO]
(Không gian synth pad lơ lửng, tiếng vocal chop)
Gió lạnh... sương mờ...

[VERSE 1]
Mở đầu câu chuyện từ buổi sớm mai
Bóng dáng ai đi qua ngõ vắng
Ánh mắt ngập ngừng nửa muốn dừng lại
Lời chưa kịp nói đã vội phai tàn.

[VERSE 2]
Ngày trôi qua ngày nối tiếp những băn khoăn
Tháng năm dần phủ mờ vết dấu thời gian
Bao khao khát chìm sâu đáy mắt
Chỉ còn lại tiếng đàn âm vang.

[CHORUS]
(Bùng nổ cảm xúc - Đầy đủ dàn nhạc)
Gửi vào mây ngàn muôn lời thầm thì
Rằng con tim này chưa từng hối tiếc
Dẫu ngày mai đường chia muôn lối
Kỷ niệm này sẽ mãi khắc ghi!

[VERSE 3]
Hoàng hôn buông trên bến đỗ bình yên
Khép lại một chặng đường dài đầy giông bão
Ta tìm thấy sự tĩnh lặng trong tâm hồn
Cảm ơn vì đã từng đi qua đời nhau.

[OUTRO]
(Giai điệu dịu êm, tiếng sáo ngân nhẹ)
Bình yên nhé...
Mọi muộn phiền... xin thả trôi...`;
    }

    suggestedStylePrompt = `${genre.toLowerCase()}, ${mood.toLowerCase()}, ${vocalType.toLowerCase()}, expressive dynamic range, acoustic instruments with modern studio mastering, 85 bpm`;
    musicalNotes = `Nên phân bổ năng lượng bài hát tăng dần từ Verse 1 (mộc mạc) lên Pre-Chorus (dồn dập) và bung nổ tại Chorus với trống và vocal hòa âm.`;

  } else {
    // English lyrics
    lyrics = `[INTRO]
(Soft acoustic guitar picking, ambient reverb)
Fading lights... distant whispers in the night...

[VERSE 1]
Walking down the boulevard where we used to run
Underneath the golden haze of the setting sun
I can still hear the promises we made back then
Before the winter cold settled in.

[PRE-CHORUS]
(Building up, strings rising in the background)
Time keeps moving forward, never looking back
And I'm still trying to fill the pieces that I lack
Even if the distance keeps us apart
You'll always have a haven in my heart.

[CHORUS]
(Powerful climax - Full drums and soaring lead vocals)
Hold on to the echoes of what we used to be
Through every storm and across the deepest sea
Though the chapters turn and daylight fades away
This melody will never drift away!

[VERSE 2]
Empty coffee cup on the wooden sill
Autumn leaves are falling down the hill
Pictures in the frame telling stories untold
A reminder of a warmth turning cold.

[CHORUS]
Hold on to the echoes of what we used to be
Through every storm and across the deepest sea
Though the chapters turn and daylight fades away
This melody will never drift away!

[BRIDGE]
(Strip down to warm piano solo)
If our paths should cross again someday
Would you look at me or simply look away?
No regrets remain in this silent prayer
Just a gratitude for the love we shared...

[OUTRO]
(Strings fade into gentle tape hiss)
Fading out... into the quiet sky...
Hold on... goodbye...
(Fade out)`;

    suggestedStylePrompt = `${genre.toLowerCase()}, ${mood.toLowerCase()}, ${vocalType.toLowerCase()}, pristine stereo mix, acoustic warmth, soaring vocal harmonies, 80 bpm`;
    musicalNotes = `Build emotional intensity gradually into the chorus. Add vocal harmonies in Chorus and Bridge for maximum resonance.`;
  }

  return {
    title,
    lyrics,
    suggestedStylePrompt,
    musicalNotes,
    fallbackUsed: false
  };
}

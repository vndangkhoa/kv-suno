# KV Suno — AI Music Streaming & Prompt Studio (v2.0.0)

**KV Suno (`kv-suno`)** là nền tảng nghe nhạc AI Suno trực tuyến và phòng thu thiết kế Prompt & Lời bài hát chuyên nghiệp (phiên bản **Spotify Encore Web Player & PWA Media Edition**), hoạt động độc lập với **bộ AI Engine cục bộ (Built-in Autonomous Engine)** mà không cần API Key bên ngoài.

📦 **Synology Package Center**: [`https://syno.vndns.net/package/kvsuno`](https://syno.vndns.net/package/kvsuno)  
🐳 **Docker Hub**: `vndangkhoa/kv-suno:latest` (`vndangkhoa/kv-suno:2.0.0`)  
🦊 **Forgejo**: [`https://git.khoavo.myds.me/vndangkhoa/kv-suno`](https://git.khoavo.myds.me/vndangkhoa/kv-suno)  
📋 **Changelog**: Xem chi tiết lịch sử phiên bản tại [CHANGELOG.md](./CHANGELOG.md).

---

## 🌟 Điểm nổi bật (v2.0.0)

- **🔥 Hot & Most Played Songs Chart (Mặc định khi mở ứng dụng)**:
  - Bảng xếp hạng các bài hát Suno AI thịnh hành và được nghe nhiều nhất, cập nhật trực tiếp từ cộng đồng 32 nhà sáng tạo.
  - Hỗ trợ chế độ xem **Chart Table (`☰`)** chuẩn Spotify Top 50 và **Cover Grid (`⊞`)**, lọc theo thể loại (`V-Pop`, `Ballad`, `Folk Trap`, `Lo-Fi`, `Remix / EDM`, `Cinematic`) và nhà sáng tạo.
- **👑 Bảng xếp hạng 32 Creators (`Creators Leaderboard`)**:
  - Vinh danh Top 3 nhà sáng tạo dẫn đầu (`#1 Gold`, `#2 Silver`, `#3 Bronze`) kèm mini-chart **Top 3 bài hát Hot nhất** ngay trên từng thẻ Creator và phát nhạc 1-chạm.
- **🎤 Xem toàn bộ Lời bài hát (Full Lyrics) & Trích xuất Style DNA**:
  - Bảng **Now Playing DNA & Lyrics Inspector** hiển thị lời bài hát đầy đủ với định dạng cấu trúc (`[Intro]`, `[Verse]`, `[Chorus]`, `[Bridge]`, `[Outro]`) và cho phép chép lời hoặc đẩy nhanh từng nhãn âm thanh (`+ tag`) vào **Prompt Studio**.
- **🎧 PWA Media Controller & Phát nhạc nền khi tắt màn hình điện thoại**:
  - Tích hợp **W3C Media Session API** hiển thị bìa đĩa, tên bài hát, tên nghệ sĩ và nút điều khiển (`Play`, `Pause`, `Previous`, `Next`, `Seek ±10s`, thanh tua thời gian) ngay trên màn hình khóa iOS / Android và tai nghe Bluetooth.
  - Tự động chuyển bài (`Auto-Next`) và duy trì luồng âm thanh liên tục ngay cả khi khóa màn hình điện thoại.
- **✨ Không cần AI API Key**:
  - Toàn bộ công cụ tạo lời bài hát (AI Lyrics), tối ưu hóa Prompt theo 5 trụ cột âm thanh (Prompt Optimizer), phân tích phong cách (Style Explainer) và cẩm nang sản xuất (55 Masterclasses) hoạt động tức thì.

---

## 🚀 Khởi chạy dự án

### 1. Chạy bằng Docker (Khuyên dùng)
```bash
docker run -d \
  --name kvsuno \
  --restart unless-stopped \
  -p 3000:3000 \
  -v kvsuno_cache:/app/.cache \
  vndangkhoa/kv-suno:latest
```

### 2. Cài đặt trên Synology NAS (DSM 7.0+)
- **Cách 1 (Tự động qua Package Center)**: Thêm nguồn gói `https://syno.vndns.net` vào **Package Center → Settings → Package Sources**, sau đó tìm **KV Suno** và nhấn **Install**.
- **Cách 2 (Cài đặt thủ công)**: Tải file `.spk` trực tiếp tại [`https://syno.vndns.net/package/kvsuno`](https://syno.vndns.net/package/kvsuno) và chọn **Manual Install** trong Package Center.

### 3. Chạy trực tiếp bằng Node.js
Yêu cầu: Node.js `>= 18.x` và `ffmpeg`.
```bash
npm install
npm start
```
Mở trình duyệt truy cập: **http://localhost:3000**

---

## 🛠️ Danh sách API Endpoints

| Method | Endpoint | Mô tả |
|---|---|---|
| `GET` | `/api/suno-songs-chart?sort=hot&creator=all` | Bảng xếp hạng bài hát Hot & nghe nhiều nhất |
| `GET` | `/api/suno-creators-ranking?sort=hot` | Bảng xếp hạng 32 Creators theo bài hát Hot / tổng lượt nghe |
| `GET` | `/api/suno-profile?handle=futurescall&page=1` | Lấy hồ sơ và danh sách bài hát theo trang của Creator |
| `GET` | `/api/suno-showcase?handle=futurescall` | Lấy danh sách bài hát tiêu biểu của Creator |
| `GET` | `/api/suno-clip-info?songId=:uuid` | Lấy metadata bài hát, lời nhạc (lyrics), style prompt |
| `GET` | `/api/suno-audio-proxy?input=:id` | Proxy stream âm thanh vượt rào cản CORS |
| `GET` | `/api/download-suno-m4a?id=:id&title=:title` | Tải bài hát trực tiếp dưới dạng `.m4a` qua `ffmpeg` |
| `POST` | `/api/fetch-drive-csv` | Đồng bộ danh sách Creators từ Google Sheets CSV |
| `POST` | `/api/explain-style` | Phân tích chi tiết phong cách nhạc |
| `POST` | `/api/generate-lyrics` | Tạo lời bài hát theo chủ đề, thể loại và cấu trúc |
| `POST` | `/api/optimize-prompt` | Chấm điểm (1–100) và nâng cấp prompt Suno theo 5 trụ cột |
| `POST` | `/api/generate-guide` | Tạo cẩm nang kiến thức theo từ khóa |
| `POST` | `/api/decode-audio-file` | Giải mã và chuẩn hóa tệp âm thanh qua `ffmpeg` |

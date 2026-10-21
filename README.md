# AU Chat Studio

Ứng dụng Android **100% ngoại tuyến** để dàn dựng kịch bản tin nhắn fanfic Alternate Universe (AU) và mô phỏng chat với thần tượng, xây dựng theo tài liệu *“AU Chat Studio — Hồ sơ thiết kế & phát triển”*.

## Tính năng

| Tính năng | Mô tả |
|---|---|
| **Tap-to-Chat** | Chế độ đọc: mỗi lần chạm hiện bong bóng “đang soạn tin…” với thời gian thật theo độ dài tin, rồi tin nhắn nảy vào. Chạm khi đang soạn để bỏ qua. Có **Tự phát** để quay màn hình làm video. |
| **3 preset nền tảng** | Lysn Bubble (pastel `#7CB8EB`, nền `#F3F6FB`, bo 18dp, đổ bóng nhẹ, viền trắng), Weverse DM (nền `#121214`, viền neon `#00E676`, gradient), Instagram DM (đen OLED, gradient hoàng hôn tím–cam, viền mảnh). Đổi bằng **1 nút**. |
| **Director Switcher** | Thanh chọn người nói một chạm ngay trên ô nhập: idol, “bạn” hoặc người dẫn truyện. |
| **Loại tin nhắn** | Văn bản, tin nhắn thoại với sóng âm vẽ bằng Canvas, ảnh, lời dẫn truyện; giờ hiển thị tùy chọn. |
| **Nhân vật** | Tên, màu vai diễn, avatar chọn bằng **Photo Picker** (không cần quyền bộ nhớ). Ảnh được sao chép vào `filesDir` nên không bao giờ mất sau khi khởi động lại máy. |
| **Biên tập** | Chạm vào tin nhắn để sửa, đổi người nói, đổi thứ tự, xóa. Chat nhóm tự hiện tên người gửi. |
| **Xuất ảnh cuộn dài** | Vẽ toàn bộ hội thoại trên Canvas ngoài màn hình, tự chia trang nếu quá dài, lưu vào `Pictures/AU Chat Studio` kèm watermark *“Nội dung hư cấu tạo bởi AU Studio”*, có nút chia sẻ. |
| **Riêng tư** | Không xin quyền INTERNET, tắt sao lưu đám mây; dữ liệu chỉ nằm trong Room/SQLite trên máy. APK không kèm ảnh hay tên thương mại của nghệ sĩ. |

## Kỹ thuật

- Kotlin 2.3, Jetpack Compose (BOM 2025.09), Material 3, Navigation type-safe
- Room 2.8 (KSP) với Flow → `collectAsStateWithLifecycle()`, kiến trúc MVVM: Entity → DAO → Repository → ViewModel → Compose
- Coil 2 cho ảnh, Coroutines
- `minSdk 31` / `targetSdk 36`: **Android 12, 13, 14, 15, 16**; edge-to-edge, predictive back, icon thích ứng có monochrome (themed icon Android 13+)
- Module `:core` thuần Kotlin chứa logic có thể kiểm thử độc lập (máy trạng thái Tap-to-Chat, preset, tiện ích)

```
core/   logic thuần Kotlin + unit test
app/    data (Room, FileStore), ui (home, characters, studio, player, chat), export
```

## Build

Yêu cầu JDK 21 (Robolectric cho SDK 36 cần Java 21) và Android SDK 36 (Android Studio bản mới).

```bash
./gradlew :core:test :app:testDebugUnitTest   # unit test + Robolectric
./gradlew :app:assembleDebug                  # APK: app/build/outputs/apk/debug/
./gradlew :app:connectedDebugAndroidTest      # kiểm thử trên máy/emulator đang cắm
```

## Kiểm thử

- `core/src/test`: máy trạng thái Tap-to-Chat, thời gian gõ, preset, tên/viết tắt, giờ, màu, chia trang xuất ảnh.
- `app/src/test` (Robolectric): Repository trên Room in-memory, ViewModel Studio & Player (thời gian ảo), renderer xuất ảnh, giao diện Compose.
- `app/src/androidTest`: luồng end-to-end trên thiết bị (tạo kịch bản, soạn tin, đổi người nói, đổi theme, sửa/xóa, Tap-to-Chat, thêm nhân vật), Room trên SQLite thật, xuất ảnh qua MediaStore.
- GitHub Actions (`.github/workflows/android.yml`) build APK, chạy unit test + lint, rồi chạy kiểm thử thiết bị trên emulator **API 31, 33, 34, 35, 36**.

## Lộ trình

Theo tài liệu thiết kế: xuất video MP4 9:16 (MediaMuxer), công cụ cắt avatar tròn, mở khóa theme trả phí một lần.

> Bản release trong CI được ký bằng khóa debug để cài thử. Trước khi đưa lên Google Play hãy tạo keystore riêng.

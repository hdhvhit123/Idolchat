# Hướng dẫn đưa Idol Chat Maker lên CH Play

Cập nhật: 17/11/2026, dựa trên `main` @ 2093d84.

Tên các mục trong Play Console được ghi theo dạng *Tiếng Việt / English*, vì giao diện Console có thể hiển thị một trong hai ngôn ngữ.

## Tình trạng hiện tại của dự án

| Mục | Trạng thái |
|---|---|
| Mã ứng dụng (`applicationId`) | `io.github.hdhvhit123.idolchat` — **không bao giờ đổi được sau khi đăng** |
| `versionCode` / `versionName` | 1 / 1.0.0 |
| `targetSdk` | 36 (Android 16) — đạt yêu cầu của Play từ 31/08/2026 |
| Ký bản release | ⚠ **Đang dùng khóa debug** → Play sẽ từ chối. Phải làm Bước 2–3 |
| Chính sách quyền riêng tư | ✅ https://hdhvhit123.github.io/Idolchat/privacy/ |
| Icon, ảnh nổi bật, ảnh màn hình | ✅ `play-store/` (tiếng Việt), `play-store/en/` (tiếng Anh) |
| Nội dung trang cửa hàng | ✅ `play-store/store-listing-vi.md`, `play-store/en/store-listing-en.md` |

Thời gian dự kiến:
- Làm hồ sơ và xác minh tài khoản: 1 đến vài ngày.
- Thử nghiệm kín bắt buộc: **ít nhất 14 ngày**.
- Google duyệt mỗi lần gửi: thường vài giờ đến vài ngày.

---

## Bước 1 — Tạo tài khoản nhà phát triển

1. Vào https://play.google.com/console/signup bằng tài khoản Google bạn muốn dùng lâu dài.
2. Chọn loại tài khoản:
   - **Cá nhân (Personal):** đơn giản nhất. Nhưng tài khoản cá nhân tạo sau 13/11/2023 **bắt buộc phải thử nghiệm kín với ít nhất 12 người trong 14 ngày liên tục** rồi mới được phát hành công khai (xem Bước 8).
   - **Tổ chức (Organization):** cần mã D-U-N-S của công ty, không phải qua 14 ngày thử nghiệm kín.
3. Trả phí đăng ký **25 USD một lần** bằng thẻ Visa hoặc Mastercard.
4. Xác minh danh tính:
   - Chụp CCCD hoặc hộ chiếu.
   - Xác minh số điện thoại và email liên hệ.
   - Tên trên giấy tờ phải khớp với tên trong hồ sơ thanh toán.
5. Điền thông tin nhà phát triển:
   - Tên nhà phát triển: là tên hiển thị trên CH Play.
   - Email liên hệ: email này **công khai** với người dùng.

## Bước 2 — Tạo khóa tải lên (upload key)

Khóa này dùng để ký file gửi lên Google. **Mất khóa thì không cập nhật app được** cho đến khi xin Google cấp lại, việc này mất vài ngày.

Trên máy Windows, mở PowerShell và chạy (JDK 21 đã có sẵn ở `C:\Users\BMTU\.jdks\jbr-21.0.11`):

```powershell
& "C:\Users\BMTU\.jdks\jbr-21.0.11\bin\keytool.exe" -genkeypair -v `
  -keystore "$env:USERPROFILE\keys\idolchat-upload.jks" `
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

- Lệnh sẽ hỏi mật khẩu và họ tên. Ghi mật khẩu vào trình quản lý mật khẩu.
- Sao lưu file `idolchat-upload.jks` ra 2 nơi, ví dụ USB và Google Drive cá nhân.
- **Không** đưa file này hay mật khẩu lên GitHub. `.gitignore` đã chặn `*.keystore` nhưng **chưa chặn `*.jks`**, nên hãy để file ngoài thư mục dự án như lệnh trên.

## Bước 3 — Cho bản release ký bằng khóa thật (cần sửa mã)

Hiện tại `app/build.gradle.kts` đang ghi `signingConfig = signingConfigs.getByName("debug")`. Cần sửa để Gradle đọc khóa từ một file nằm ngoài git. Việc sửa mã này do phiên trên cloud làm.

**3a. Tạo file `keystore.properties`** ở thư mục gốc dự án và thêm dòng `keystore.properties` vào `.gitignore`:

```properties
storeFile=C:/Users/BMTU/keys/idolchat-upload.jks
storePassword=MẬT_KHẨU
keyAlias=upload
keyPassword=MẬT_KHẨU
```

**3b. Sửa `app/build.gradle.kts`:**

```kotlin
val keystoreProps = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    signingConfigs {
        create("upload") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // CI không có khóa thật thì vẫn ký debug để build được.
            signingConfig = if (keystoreProps.isNotEmpty()) signingConfigs.getByName("upload")
                            else signingConfigs.getByName("debug")
        }
    }
}
```

## Bước 4 — Build file AAB

CH Play chỉ nhận file **.aab** (Android App Bundle), không nhận APK, cho app mới.

```powershell
cd C:\Users\BMTU\AndroidStudioProjects\Idolchat
$env:JAVA_HOME = "C:\Users\BMTU\.jdks\jbr-21.0.11"
.\gradlew.bat :app:bundleRelease
```

- File kết quả nằm ở `app\build\outputs\bundle\release\app-release.aab`.
- Trong Android Studio có thể làm tương đương: *Build → Generate Signed App Bundle / APK → Android App Bundle*.
- **Mỗi lần gửi bản mới phải tăng `versionCode`** trong `app/build.gradle.kts` (1 → 2 → 3…) và đổi `versionName` (1.0.0 → 1.0.1…).

## Bước 5 — Tạo ứng dụng trong Play Console

*Tạo ứng dụng / Create app*:

| Ô | Điền |
|---|---|
| Tên ứng dụng | `Idol Chat Maker: Tạo chat idol` |
| Ngôn ngữ mặc định | Tiếng Việt – vi-VN |
| Ứng dụng hay trò chơi | Ứng dụng |
| Miễn phí hay có phí | Miễn phí (**sau này không đổi sang có phí được**) |
| Cam kết | Tích cả hai ô: Chính sách chương trình dành cho nhà phát triển và Luật xuất khẩu Hoa Kỳ |

## Bước 6 — Hoàn thành mục *Thiết lập ứng dụng / Set up your app*

Mở *Bảng điều khiển / Dashboard*, rồi làm lần lượt từng mục:

1. **Chính sách quyền riêng tư / Privacy policy:** dán `https://hdhvhit123.github.io/Idolchat/privacy/`.
2. **Quyền truy cập ứng dụng / App access:** chọn *Mọi chức năng đều dùng được mà không cần quyền truy cập đặc biệt* (app không có đăng nhập).
3. **Quảng cáo / Ads:** *Không, ứng dụng không chứa quảng cáo*.
4. **Mức phân loại nội dung / Content rating:** điền bảng hỏi IARC.
   - Email: email liên hệ của bạn.
   - Danh mục: *Tất cả các loại khác / All other app types*.
   - Bạo lực, tình dục, ngôn từ thô tục, chất cấm, cờ bạc: **Không**.
   - Câu hỏi về người dùng tương tác hoặc trao đổi nội dung với nhau: **Không**. App không có chat thật giữa người dùng; chia sẻ ảnh chỉ đi qua bảng chia sẻ của Android.
5. **Đối tượng mục tiêu / Target audience:**
   - Chọn **13–15, 16–17, 18+**.
   - **Không chọn dưới 13** để tránh chương trình Gia đình (Families), vốn có yêu cầu khắt khe hơn.
6. **Ứng dụng tin tức / News apps:** Không.
7. **An toàn dữ liệu / Data safety:**
   - *Ứng dụng có thu thập hoặc chia sẻ dữ liệu người dùng không?* → **Không**.
   - Lý do: app không có quyền Internet, không có SDK quảng cáo hay phân tích, dữ liệu chỉ nằm trên máy. Theo hướng dẫn của Google, dữ liệu chỉ xử lý trên thiết bị và không gửi đi thì không tính là "thu thập".
8. **Các mục còn lại:** Ứng dụng chính phủ, Tính năng tài chính, Ứng dụng sức khỏe → đều chọn Không / Không áp dụng.
9. **Danh mục ứng dụng và thông tin liên hệ / App category & contact details:**
   - Danh mục: **Giải trí (Entertainment)**.
   - Email liên hệ công khai.
   - Trang web (không bắt buộc): `https://hdhvhit123.github.io/Idolchat/`.

## Bước 7 — Trang thông tin chính / Main store listing

Mở *Phát triển số người dùng → Trang thông tin trong cửa hàng → Trang thông tin chính* (*Grow → Store presence → Main store listing*):

| Ô | Lấy từ |
|---|---|
| Tên ứng dụng (≤30) | `store-listing-vi.md` → `Idol Chat Maker: Tạo chat idol` |
| Mô tả ngắn (≤80) | `store-listing-vi.md` |
| Mô tả đầy đủ (≤4000) | `store-listing-vi.md` |
| Biểu tượng ứng dụng | `play-store/icon_512.png` |
| Hình ảnh nổi bật | `play-store/feature_1024x500.png` |
| Ảnh chụp màn hình điện thoại | `play-store/01_studio.png` … `07_characters.png`, đăng theo đúng thứ tự |
| Ảnh máy tính bảng | Bỏ trống — app chưa tối ưu cho máy tính bảng |

**Thêm tiếng Anh:**
1. Bấm *Quản lý bản dịch → Thêm bản dịch của riêng bạn* (*Manage translations → Add your own translations*).
2. Chọn **English (United States) – en-US**.
3. Dán nội dung từ `play-store/en/store-listing-en.md`.
4. Tải ảnh từ `play-store/en/` lên.

## Bước 8 — Thử nghiệm nội bộ, rồi thử nghiệm kín (bắt buộc với tài khoản cá nhân)

**8a. Thử nghiệm nội bộ / Internal testing** — không bắt buộc nhưng nên làm để thử nhanh, bản cài được trong vài phút.
1. Vào *Kiểm thử → Thử nghiệm nội bộ → Tạo bản phát hành mới*.
2. Lần đầu, Console hỏi về **Play App Signing**. Chọn **Để Google quản lý và bảo vệ khóa ký ứng dụng (khuyên dùng)**. Google giữ khóa ký thật; bạn chỉ giữ khóa tải lên ở Bước 2.
3. Tải `app-release.aab` lên. Tên bản phát hành để mặc định `1 (1.0.0)`.
4. Điền ghi chú phát hành, lấy từ phần "Ghi chú phát hành" trong `store-listing-vi.md`.
5. Ở tab *Người thử nghiệm*, thêm email của bạn. Mở đường link mời bằng điện thoại để cài từ CH Play.

**8b. Thử nghiệm kín / Closed testing — ít nhất 12 người, 14 ngày liên tục**
1. Vào *Kiểm thử → Thử nghiệm kín → Tạo bản thử nghiệm*, hoặc dùng sẵn kênh *Alpha*.
2. Thêm người thử nghiệm bằng danh sách email hoặc một Google Group.
   - Nên mời khoảng 15–20 người để có dư.
   - Người thử phải dùng tài khoản Google trên điện thoại Android.
3. Tải lên đúng AAB đó (hoặc bản mới hơn với `versionCode` cao hơn), chọn quốc gia, rồi **Gửi để xem xét**.
4. Khi Google duyệt xong, gửi **đường link tham gia (opt-in)** cho người thử. Họ phải:
   - mở link, bấm *Trở thành người thử nghiệm*;
   - cài app từ CH Play;
   - **giữ trạng thái tham gia suốt 14 ngày** và thỉnh thoảng mở app dùng thật. Google xem cả mức độ tương tác.
5. Không tạm dừng kênh thử nghiệm trong 14 ngày đó.
6. Sau 14 ngày, trên Dashboard bấm **Đăng ký quyền phát hành chính thức / Apply for production**. Trả lời bảng hỏi về:
   - cách bạn tuyển người thử nghiệm;
   - phản hồi họ đưa ra;
   - những gì bạn đã sửa;
   - vì sao app đã sẵn sàng.
   Google thường trả lời trong khoảng 7 ngày.

## Bước 9 — Phát hành chính thức / Production

1. Vào *Phát hành chính thức → Tạo bản phát hành mới*. Chọn lại AAB đã thử (*Thêm từ thư viện*) hoặc tải bản mới.
2. Ở tab *Quốc gia/khu vực*, chọn **Việt Nam**, và thêm các nước khác nếu muốn. Muốn bán khắp nơi thì chọn tất cả.
3. Có thể phát hành theo **tỷ lệ phần trăm (staged rollout)**, ví dụ 20%, để theo dõi lỗi trước khi mở cho tất cả.
4. Vào *Tổng quan về bản phát hành → Gửi thay đổi để xem xét*.
5. Lần đầu Google duyệt thường mất **vài ngày, có khi đến 7 ngày**. Khi duyệt xong, app hiện trên CH Play sau vài giờ.

## Bước 10 — Cập nhật sau này

1. Sửa code.
2. **Tăng `versionCode`** và đổi `versionName`.
3. Chạy `.\gradlew.bat :app:bundleRelease`.
4. Lên Console, tạo bản phát hành mới ở kênh Phát hành chính thức (hoặc thử ở kênh nội bộ trước), tải AAB lên, viết ghi chú, rồi gửi xem xét.

Bản cập nhật **không** phải chạy lại 14 ngày thử nghiệm kín.

---

## ⚠ Rủi ro bị từ chối nên xử lý trước khi nộp

1. **Tên thương hiệu trong app:**
   - Ba phong cách đang tên "Lysn Bubble", "Weverse DM", "Instagram DM".
   - Các tên này hiện trong app, trên ảnh xuất ra và trong 5/7 ảnh màn hình.
   - Google có thể từ chối theo chính sách *Mạo danh* và *Sở hữu trí tuệ*. Khi đó cần đổi tên rồi chụp lại ảnh (script đã có sẵn).
2. **Tên người thật trong truyện mẫu:**
   - Truyện mẫu tự tạo khi mở app lần đầu dùng "Minho (Bias)" và "Hyunjin", trùng tên thành viên nhóm nhạc có thật.
   - Nên đổi sang "Idol", "Thành viên" như các mẫu khác.
3. **Ảnh màn hình phải đúng với app.** Giao diện app đang là tiếng Việt; trong mô tả tiếng Anh đã ghi rõ điều này.
4. **Email liên hệ phải nhận được thư.** Google gửi thông báo duyệt và cảnh báo chính sách qua email này.

## Lỗi thường gặp khi tải lên

| Thông báo | Cách xử lý |
|---|---|
| *You uploaded an APK or Android App Bundle that was signed in debug mode* | Chưa làm Bước 3 |
| *Version code 1 has already been used* | Tăng `versionCode` |
| *Your app must target API level 36 or higher* | Dự án đã ở 36, không cần làm gì |
| *The Android App Bundle was not signed* / sai khóa | Ký bằng đúng khóa tải lên đã đăng ký ở lần đầu |
| Thiếu mục Data safety hoặc Content rating | Làm đủ các mục ở Bước 6 (Dashboard có dấu tích) |

## Nguồn tham khảo
- Yêu cầu target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Yêu cầu thử nghiệm kín cho tài khoản cá nhân mới: https://support.google.com/googleplay/android-developer/answer/14151465
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756

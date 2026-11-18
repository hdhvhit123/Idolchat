# Hướng dẫn đưa Idol Chat Maker lên CH Play (tài khoản Play Console tạo năm 2017)

Cập nhật: 18/11/2026, dựa trên `main` @ c92833e (đã có giao diện mới và 12 ngôn ngữ).

Tên mục trong Play Console ghi dạng *Tiếng Việt / English*.

## Tài khoản từ năm 2017 khác gì tài khoản mới

| | Tài khoản mới (cá nhân, sau 13/11/2023) | **Tài khoản của bạn (2017)** |
|---|---|---|
| Phí đăng ký 25 USD | Phải trả | **Không** — đã trả từ trước |
| Thử nghiệm kín 12 người × 14 ngày trước khi phát hành | Bắt buộc | **Không bắt buộc** — được phát hành thẳng lên kênh chính thức |
| Xác minh danh tính, email, số điện thoại | Làm khi đăng ký | Làm nếu Console còn hiện yêu cầu (xem Bước 1) |

Nhờ vậy, từ lúc có file AAB đến khi app lên CH Play chỉ còn chờ Google duyệt, thường là vài ngày.

## Tình trạng dự án

| Mục | Trạng thái |
|---|---|
| Mã ứng dụng | `io.github.hdhvhit123.idolchat` — **không đổi được sau khi đăng** |
| `versionCode` / `versionName` | 1 / 1.0.0 |
| `targetSdk` | 36 — đạt yêu cầu Play từ 31/08/2026 |
| Ngôn ngữ app | 12: en (mặc định), vi, zh-Hans, zh-Hant, ko, ja, hi, th, in, es, pt, fr — tự theo ngôn ngữ máy |
| Ký bản release | ⚠ **Vẫn dùng khóa debug** → Play từ chối. Phải làm Bước 2–3 |
| Chính sách quyền riêng tư | ✅ https://hdhvhit123.github.io/Idolchat/privacy/ |
| Icon, ảnh nổi bật, ảnh màn hình | ✅ `play-store/` (vi), `play-store/en/` (en) |
| Nội dung trang cửa hàng | ✅ `store-listing-vi.md`, `en/store-listing-en.md` |

---

## Bước 1 — Kiểm tra tài khoản

1. Vào https://play.google.com/console bằng tài khoản Google đã tạo năm 2017.
2. Mở **Trang chủ** và **Hộp thư** (biểu tượng chuông). Nếu có yêu cầu **xác minh danh tính / Verify your identity** hoặc **xác minh thông tin liên hệ**, làm theo cho xong. Google yêu cầu cả tài khoản cũ xác minh; chưa xong thì không phát hành được.
3. Mở *Cài đặt → Chi tiết về nhà phát triển* (*Settings → Developer account → About you*) và kiểm tra:
   - **Tên nhà phát triển**: hiển thị trên CH Play.
   - **Email liên hệ**: công khai cho người dùng và phải nhận được thư.
   - **Website**: không bắt buộc, có thể dùng `https://hdhvhit123.github.io/Idolchat/`.
4. **Khai báo tư cách người bán** (theo luật EU):
   - App miễn phí, không quảng cáo → chọn **Pháp nhân phi thương mại**.
   - Nếu gắn AdMob hoặc thu tiền → chọn **Người bán**; thông tin liên hệ sẽ hiển thị công khai ở EU.

## Bước 2 — Tạo khóa tải lên (upload key)

Mất khóa thì không cập nhật app được cho đến khi xin Google cấp lại, mất vài ngày.

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\keys" | Out-Null
& "<thư mục JDK 21>\bin\keytool.exe" -genkeypair -v `
  -keystore "$env:USERPROFILE\keys\idolchat-upload.jks" `
  -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

- Thư mục JDK 21: trên máy BMTU là `C:\Users\BMTU\.jdks\jbr-21.0.11`. Trong Android Studio xem ở *Settings → Build Tools → Gradle → Gradle JDK*.
- Ghi mật khẩu vào trình quản lý mật khẩu.
- Sao lưu file `.jks` ra 2 nơi.
- **Không** để file này trong thư mục dự án. `.gitignore` hiện chỉ chặn `*.keystore`, không chặn `*.jks`.

## Bước 3 — Cho bản release ký bằng khóa thật (cần sửa mã)

`app/build.gradle.kts` đang ghi `signingConfig = signingConfigs.getByName("debug")`.

**3a.** Tạo file `keystore.properties` ở thư mục gốc dự án, thêm dòng `keystore.properties` và `*.jks` vào `.gitignore`:

```properties
storeFile=C:/Users/<tên user>/keys/idolchat-upload.jks
storePassword=MẬT_KHẨU
keyAlias=upload
keyPassword=MẬT_KHẨU
```

**3b.** Sửa `app/build.gradle.kts`:

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

```powershell
git pull origin main
.\gradlew.bat :app:bundleRelease
```

- Kết quả nằm ở `app\build\outputs\bundle\release\app-release.aab`.
- Trong Android Studio: *Build → Generate Signed App Bundle / APK → Android App Bundle*.
- **Mỗi lần gửi bản mới phải tăng `versionCode`** (1 → 2 → 3…) và đổi `versionName` (1.0.0 → 1.0.1…).

## Bước 5 — Tạo ứng dụng

*Tất cả ứng dụng → Tạo ứng dụng / All apps → Create app*:

| Ô | Điền |
|---|---|
| Tên ứng dụng | `Idol Chat Maker: Chat Stories` (nếu ngôn ngữ mặc định là tiếng Anh) hoặc `Idol Chat Maker: Tạo chat idol` (nếu là tiếng Việt) |
| Ngôn ngữ mặc định | **English (United States) – en-US** là khuyên dùng, vì app mặc định tiếng Anh và phát hành nhiều nước. Thêm tiếng Việt làm bản dịch ở Bước 7 |
| Ứng dụng / Trò chơi | Ứng dụng |
| Miễn phí / Có phí | Miễn phí — sau này **không đổi sang có phí được** |
| Cam kết | Tích cả hai ô |

## Bước 6 — Thiết lập ứng dụng / Set up your app (trên Dashboard)

1. **Chính sách quyền riêng tư:** `https://hdhvhit123.github.io/Idolchat/privacy/`
2. **Quyền truy cập ứng dụng:** Mọi chức năng dùng được mà không cần quyền truy cập đặc biệt (app không có đăng nhập).
3. **Quảng cáo:** Không.
   - Nếu sau này gắn AdMob: chọn Có, đồng thời sửa chính sách và mục An toàn dữ liệu.
4. **Mức phân loại nội dung (IARC):**
   - Danh mục: *Tất cả các loại khác*.
   - Bạo lực, tình dục, ngôn từ thô tục, chất cấm, cờ bạc: Không.
   - Người dùng tương tác hoặc trao đổi nội dung với nhau: Không.
5. **Đối tượng mục tiêu:** chọn 13–15, 16–17, 18+. **Không** chọn dưới 13.
6. **Ứng dụng tin tức:** Không.
7. **An toàn dữ liệu:** *Ứng dụng có thu thập hoặc chia sẻ dữ liệu người dùng?* → **Không**.
   - Lý do: app không có quyền Internet, không có SDK bên thứ ba thu dữ liệu, dữ liệu chỉ nằm trên máy.
8. **Ứng dụng chính phủ / Tính năng tài chính / Sức khỏe:** Không.
9. **Danh mục:** Giải trí (Entertainment).
   - Email liên hệ.
   - Website: `https://hdhvhit123.github.io/Idolchat/` (không bắt buộc).

## Bước 7 — Trang thông tin chính / Main store listing

| Ô | en-US (mặc định) | vi-VN (bản dịch) |
|---|---|---|
| Tên, mô tả ngắn, mô tả đầy đủ | `en/store-listing-en.md` | `store-listing-vi.md` |
| Biểu tượng 512×512 | `icon_512.png` | (dùng chung) |
| Hình ảnh nổi bật 1024×500 | `en/feature_1024x500.png` | `feature_1024x500.png` |
| Ảnh chụp màn hình điện thoại | `en/01…07.png` | `01…07.png` |

- Bản tiếng Việt: *Quản lý bản dịch → Thêm bản dịch của riêng bạn → Tiếng Việt*.
- Các ngôn ngữ khác (zh, ko, ja, hi…) có thể thêm sau. Người dùng các nước đó sẽ thấy trang tiếng Anh, nhưng app vẫn hiện đúng ngôn ngữ máy của họ.
- Mô tả tiếng Việt và tiếng Anh đã ghi app có 12 ngôn ngữ.
- ⚠ Ảnh trong `en/` vẫn chụp giao diện tiếng Việt (chụp trước khi có bản đa ngôn ngữ) → nên chụp lại bằng giao diện tiếng Anh trước khi nộp. CI đã có ảnh theo từng ngôn ngữ ở nhánh `ci-screenshots-locales`.

## Bước 8 — Thử nhanh (khuyên làm, không bắt buộc)

1. Mở *Kiểm thử → Thử nghiệm nội bộ → Tạo bản phát hành mới*.
2. Lần tải đầu tiên, Console hỏi về **Play App Signing** → chọn **Để Google quản lý và bảo vệ khóa ký ứng dụng**.
3. Tải `app-release.aab` lên, ghi chú phát hành, lưu và phát hành.
4. Ở tab *Người thử nghiệm*, thêm email của bạn. Mở link mời trên điện thoại để cài từ CH Play.
5. Kiểm tra app mở được, đổi ngôn ngữ máy thì app đổi theo, xuất ảnh được.

Bước này chỉ mất vài phút đến vài giờ, giúp phát hiện lỗi ký hay lỗi bản release trước khi lên chính thức.

## Bước 9 — Phát hành chính thức / Production

Tài khoản 2017 không phải chờ 14 ngày thử nghiệm kín.

1. Mở *Phát hành chính thức → Tạo bản phát hành mới*.
2. Chọn AAB: *Thêm từ thư viện* (bản đã thử ở Bước 8) hoặc tải lên mới.
3. Viết ghi chú phát hành cho en-US và vi-VN.
4. Tab *Quốc gia/khu vực*: chọn các nước muốn phát hành. App có 12 ngôn ngữ nên có thể chọn tất cả.
5. Có thể phát hành dần (*staged rollout*), ví dụ 20% rồi tăng dần.
6. Mở *Tổng quan về bản phát hành → Gửi thay đổi để xem xét*.
7. Chờ Google duyệt: lần đầu thường vài ngày, có khi đến 7 ngày. Theo dõi email và Hộp thư trong Console.

## Bước 10 — Cập nhật sau này

Sửa code → tăng `versionCode` → `bundleRelease` → tạo bản phát hành mới ở kênh chính thức → gửi xem xét.

## ⚠ Rủi ro bị từ chối nên xử lý trước

1. **Tên thương hiệu:** tên ba phong cách "Lysn Bubble", "Weverse DM", "Instagram DM" hiện trong app, ảnh xuất và ảnh cửa hàng. Có thể bị từ chối theo chính sách Mạo danh / Sở hữu trí tuệ.
2. **Tên người thật:** truyện mẫu tự tạo dùng "Minho (Bias)", "Hyunjin", trùng tên thành viên nhóm nhạc có thật.
3. **Nội dung cửa hàng khớp với app:** chụp lại ảnh tiếng Anh như ghi ở Bước 7.

## Lỗi thường gặp

| Thông báo | Cách xử lý |
|---|---|
| *signed in debug mode* | Chưa làm Bước 3 |
| *Version code 1 has already been used* | Tăng `versionCode` |
| *You need to verify your identity* / không bấm được Gửi xem xét | Hoàn tất xác minh ở Bước 1 |
| *The Android App Bundle was signed with the wrong key* | Dùng đúng khóa tải lên đã dùng lần đầu |
| Dashboard còn mục chưa tích | Làm đủ Bước 6 |

## Nguồn
- Yêu cầu target API: https://support.google.com/googleplay/android-developer/answer/11926878
- Thử nghiệm kín chỉ bắt buộc với tài khoản cá nhân tạo sau 13/11/2023: https://support.google.com/googleplay/android-developer/answer/14151465
- Play App Signing: https://support.google.com/googleplay/android-developer/answer/9842756

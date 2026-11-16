# Ảnh chụp thiết bị — nhánh `ui-upgrade` @ cd1bc0f (sau nâng cấp)

- Thiết bị: máy ảo Pixel 7 (1080×2400, 420 dpi), Android 16 (API 36), cùng máy ảo với bộ ảnh `before/`.
- Build: `gradlew.bat :app:assembleDebug` bằng JDK 21 — **thành công** (5 phút 39 giây, 1 cảnh báo Kotlin `w:`).
- Cài: `adb install -r` đè lên bản `main` 12edf51 đang có dữ liệu, **không gỡ app**.
- Không có crash trong toàn bộ phiên (`logcat -b crash` trống).

## Bảo toàn dữ liệu khi cài đè — ĐẠT

- Trước khi cài: 2 kịch bản, 12 tin nhắn, 3 nhân vật (đọc trực tiếp Room DB bằng `run-as … sqlite3`).
- Sau khi cài và mở app: dump `INSERT` của 3 bảng `stories`, `messages`, `characters` **giống hệt từng dòng** (`diff` không khác).
- Không tạo thêm truyện mẫu: app ghi `sample_seeded=true` vào `au_studio_prefs.xml` khi thấy đã có dữ liệu.
- Schema Room vẫn version 1, `1.json` sinh ra giống hệt bản build từ `main` → đã chép vào `device-screenshots/schema/`.

## Kết quả thử tính năng mới

| Tính năng | Kết quả | Ảnh |
|---|---|---|
| Bấm mẫu "Chúc ngủ ngon" ở "Bắt đầu nhanh từ mẫu" | ĐẠT — mở Studio, 7 tin, dùng lại nhân vật có sẵn (không tạo trùng) | 25 |
| Mẫu khi không còn nhân vật nào ("Sau concert") | ĐẠT — tạo kịch bản 7 tin, tự tạo nhân vật "Y/N (Bạn)" và "Idol" | 43 |
| Menu ⋮ thẻ kịch bản → Nhân bản | ĐẠT — "Chúc ngủ ngon (bản sao)", đủ 8 tin kể cả tin ảnh; file ảnh được sao riêng (`msg_copy_…webp`); snackbar "Đã tạo bản sao" | 29, 30 |
| Xóa tin → "Hoàn tác" trên snackbar | ĐẠT — DB 10 → 9 → 10 tin | 12 |
| Chọn "Dẫn truyện" → chip "Hôm sau" | ĐẠT — chip gửi ngay lời dẫn "Hôm sau" vào hội thoại (không chỉ điền vào ô) | 09, 10 |
| Đổi phong cách 2 lần liên tiếp | ĐẠT — không còn snackbar tên cũ (đổi phong cách không hiện snackbar nữa; tên phong cách hiện ở dòng phụ header) | 07 |
| Nút "Phát thử" cuối kịch bản | ĐẠT — mở Tap-to-Chat | 16 |
| Nút "Xuất ảnh" cuối kịch bản | ĐẠT — lưu PNG vào Pictures/AU Chat Studio; nút "Chia sẻ" trên snackbar mở bảng chia sẻ hệ thống | 14, 15 |
| Đóng thẻ "3 bước…" rồi mở lại app | ĐẠT — không hiện lại (`guide_dismissed=true`) | 38 |
| Xóa hết kịch bản và nhân vật rồi mở lại app | ĐẠT — 0 kịch bản / 0 nhân vật / 0 tin; truyện mẫu KHÔNG tự tạo lại; file avatar mồ côi đã được dọn khỏi `files/images` | 40, 41, 42 |
| Photo Picker chọn avatar | ĐẠT — mở Photo Picker hệ thống, ảnh lưu `files/images/avatar_…webp`, hiển thị ở Dàn nhân vật, Studio, Trang chủ | 22, 23, 24 |
| Photo Picker gửi ảnh trong Studio | ĐẠT — tin ảnh hiển thị, file `msg_…webp` | 28 |
| Xoay ngang khi đang gõ dở | ĐẠT — "Draft before rotate" còn nguyên khi xoay ngang và xoay lại | 26, 27 |
| Hộp thoại xác nhận xóa kịch bản / nhân vật | ĐẠT — nêu rõ số tin bị xóa | 39 |

## Đối chiếu 14 vấn đề ở giai đoạn 1

| # | Vấn đề | Trạng thái | Ghi chú / ảnh |
|---|---|---|---|
| 1 | Huy hiệu đổi ảnh trên avatar bị cắt | **Đã hết** | 21 |
| 2 | Snackbar đổi phong cách hiện tên cũ | **Đã hết** | 07 |
| 3 | Thanh người nói cuộn lệch, người đang chọn bị khuất | **Đã hết** | người đang chọn đứng đầu, viền màu (05, 06, 08). Placeholder vẫn bị cắt "Nhắn với tư cách Minho (…" |
| 4 | Thanh trượt độ dài tin nhắn thoại hiển thị sai | **Còn** | con trượt vẫn sát mép trái khi giá trị 0:08, track hồng nhạt (31) |
| 5 | Lysn: chữ trắng trên bong bóng xanh pastel | **Đã hết** | chữ tối trên xanh (06, 25) |
| 6 | Phong cách tối: header/ô nhập sáng, chat đen | **Đã hết** | header + thanh nhập theo phong cách (05, 08) |
| 7 | Tap-to-Chat mở đầu màn đen trống; kết thúc thiếu Xuất/Chia sẻ; nút lạc tông | **Phần lớn đã hết** | có "Chạm để bắt đầu" + số tin (16); "Xem lại" theo màu phong cách (19). Màn kết thúc vẫn chưa có Xuất/Chia sẻ |
| 8 | Xuất ảnh bị giấu trong menu ⋮ | **Đã hết** | nút Phát thử / Xuất ảnh ở cuối kịch bản (05, 14) |
| 9 | Icon gọi/video không làm gì | **Còn một phần** | đã bỏ khỏi header Studio; vẫn còn ở header Tap-to-Chat (16) — chấp nhận được nếu chỉ để trang trí khi quay video |
| 10 | "Đổi người nói" thiếu "Dẫn truyện" | **Còn** | bảng thao tác chỉ có 3 nhân vật (13) |
| 11 | Nút Xóa nhân vật cùng màu Hủy | **Đã hết** | Xóa màu đỏ (21) |
| 12 | TalkBack đọc "Người nói: Thêm" | **Đã hết** | nay là "Thêm nhân vật" |
| 13 | Cỡ chữ 130% làm gãy dòng chip | **Đã hết** | không còn hàng chip; ở 130% chỉ cắt chữ hợp lý (36, 37) |
| 14 | Thứ tự nhảy khi chỉ đổi phong cách; phụ đề tin cuối dễ hiểu nhầm | **Còn một phần** | thẻ "VIẾT TIẾP" lấy kịch bản sửa gần nhất nên vẫn nhảy khi chỉ đổi phong cách; phụ đề vẫn là tin cuối ("🖼️ Ảnh", "Hyunjin đã xem") nhưng nay có nhãn "VIẾT TIẾP" nên dễ hiểu hơn |

## Lỗi mới phát hiện

1. **Biểu tượng thanh trạng thái sai màu trong Tap-to-Chat** (16, 19, 33) — mức trung bình, dễ thấy.
   - Weverse/Instagram (nền tối) ở chế độ sáng hệ thống: giờ/pin màu tối trên nền tối, gần như không thấy. `dumpsys window` trong lúc đó: `mLastAppearance=LIGHT_STATUS_BARS`.
   - Lysn (nền sáng) ở chế độ tối hệ thống: giờ/pin màu trắng trên nền trắng (33).
   - Studio thì đúng màu. Nghi là `SystemBarsForBackground` của Studio chạy `onDispose` (khôi phục giá trị cũ) **sau** khi Player đã đặt giá trị mới lúc chuyển màn → Player bị ghi đè. (Kết luận từ quan sát, chưa đọc mã.)
2. **Nút nổi "Kịch bản mới" che mục "Thêm" của hàng Dàn nhân vật** ở Trang chủ khi chưa cuộn (01, 34); ở cỡ chữ 130% che chữ thẻ mẫu (36). Cần đệm cuối danh sách hoặc thu nhỏ FAB sớm hơn.
3. Dòng phụ header Studio khi tiêu đề bị cắt có khoảng trống lạ trước dấu "·": "Tin nhắn lúc n…  · Lysn Bubble" (06, 08).
4. Tap-to-Chat phong cách Lysn ở chế độ tối hệ thống: công tắc "Tự phát" dùng track tối của theme hệ thống trên thanh sáng (33) — lệch tông nhẹ.
5. Snackbar "Đã lưu ảnh … / Chia sẻ" tự tắt sau khoảng 4 giây và che hai nút Phát thử/Xuất ảnh (15) — hơi ngắn cho thao tác Chia sẻ.

## Chưa kiểm tra được

- TalkBack thực tế (chỉ kiểm qua nhãn accessibility trong uiautomator).
- Thiết bị thật, màn hình nhỏ (< 400 dp), máy tính bảng.
- Đổi tên kịch bản; nút Chia sẻ tới app khác thật (chỉ mở bảng chia sẻ, không gửi).
- Hiệu năng bản release. Bản debug trên máy ảo: mở nguội 2,1–2,2 s (bản `main` trước đó 1,8–2,3 s) — chỉ tham khảo, không phải số đo so sánh.

## Danh sách ảnh

01–03 Trang chủ (đầu, cuộn, cuối) · 04 Kịch bản mới · 05/06/08 Studio Instagram/Lysn/Weverse · 07 sau khi đổi phong cách 2 lần ·
09–10 Dẫn truyện + chip "Hôm sau" · 11/13 bảng thao tác (lời dẫn / tin nhân vật) · 12 snackbar Hoàn tác · 14 Xuất ảnh · 15 snackbar Chia sẻ ·
16–19 Tap-to-Chat đầu/đang soạn/giữa/cuối · 20–24 Dàn nhân vật, sửa, Photo Picker, avatar mới · 25 mẫu "Chúc ngủ ngon" ·
26–27 xoay ngang/dọc khi gõ dở · 28 gửi ảnh · 29–30 menu thẻ + Nhân bản · 31 hộp thoại tin nhắn thoại ·
32–35 chế độ tối (Studio Lysn, Tap-to-Chat, Trang chủ, Dàn nhân vật) · 36–37 cỡ chữ 130% · 38 sau khi đóng hướng dẫn và mở lại ·
39 xác nhận xóa · 40–42 không còn kịch bản/nhân vật, mở lại app · 43 mẫu khi trống.

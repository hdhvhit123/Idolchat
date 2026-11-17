# Kiểm tra bản đổi tên "Idol Chat Maker" — `ui-upgrade` @ 05ac40e

- Máy ảo Pixel 7, Android 16 (API 36). Build `assembleDebug` bằng JDK 21: **thành công** (50 giây, build tăng dần).
- Cài đè `adb install -r` lên bản cd1bc0f, không gỡ app. Không có crash (`logcat -b crash` trống).

## Bảo toàn dữ liệu — ĐẠT
- Trước khi cài: 2 kịch bản ("Sau concert", "Nhóm chat giờ nghỉ") và các tin nhắn, nhân vật của chúng — 18 dòng `INSERT`.
- Sau khi cài và mở app: 18 dòng **giống hệt** (`diff` không khác). Truyện mẫu không tự tạo.
- Trong lúc thử tôi lỡ bấm thẻ mẫu "Sau concert" nên tạo thêm 1 kịch bản (id 7); đã xóa qua giao diện. Dump cuối buổi giống hệt dump trước khi cài.

## Đổi tên — ĐẠT
| Kiểm tra | Kết quả | Ảnh |
|---|---|---|
| Nhãn dưới icon | "Idol Chat", hiện đủ, không bị cắt | 01 |
| Trang chủ sáng | Tiêu đề "Idol Chat Maker" + dòng phụ "Tạo truyện chat idol, xuất thành ảnh" | 02 |
| Trang chủ tối | Như trên, đủ tương phản | 03 |
| Trang chủ cỡ chữ 130% | Tiêu đề và dòng phụ hiện đủ một dòng | 04 |
| Xuất ảnh | Snackbar "Đã lưu ảnh vào Pictures/Idol Chat Maker"; file nằm trong `/sdcard/Pictures/Idol Chat Maker/` | 05 |
| Watermark | "⚠ Nội dung hư cấu tạo bởi Idol Chat Maker" | 06 |

Ảnh xuất trước đây vẫn nằm ở `Pictures/AU Chat Studio` (không bị di chuyển hay xóa).

## Các bản sửa từ d32ae0e
| Bản sửa | Kết quả | Ảnh |
|---|---|---|
| Màu thanh trạng thái Studio → Phát → quay lại, Weverse, máy ở chế độ sáng | **ĐẠT** — icon trắng trên nền tối ở cả 3 bước (`mLastAppearance` không có cờ LIGHT); về Trang chủ trở lại icon tối | 07, 08, 09 |
| Như trên, Lysn, máy ở chế độ tối | **ĐẠT** — icon tối trên nền sáng ở cả 3 bước (`LIGHT_STATUS_BARS`) | 10, 11, 12 |
| Công tắc "Tự phát" khi xem Lysn ở chế độ tối | **ĐẠT** — track sáng, hợp tông | 11 |
| "Dẫn truyện" trong "Đổi người nói" | **ĐẠT** — có mục "Dẫn truyện" sau các nhân vật | 13 |
| Nhãn hai đầu thanh trượt tin thoại | **ĐẠT** — "0:01" và "2:00". Với dải tới 2:00, giá trị 0:08 nằm sát mép trái là **đúng**; vấn đề #4 ở giai đoạn 1 phần lớn do tôi đọc nhầm khi thiếu nhãn. Track chưa chọn ở chế độ tối màu đỏ mận đậm, hơi nặng | 14 |
| Nút "Thêm" ở đầu hàng Dàn nhân vật | **ĐẠT** — "Thêm" đứng đầu, không còn bị FAB che | 02, 03 |
| Dòng phụ header Studio | **ĐẠT** — dạng "Phong cách · Tên kịch bản" ("Weverse DM · Sau concert", "Lysn Bubble · Nhóm chat giờ …"), không còn khoảng trống lạ | 10, 14 |

## Còn lại / quan sát thêm
- Nút nổi "Kịch bản mới" vẫn che phần cuối thẻ trong "Kịch bản khác" khi chưa cuộn ("7 tin nhắn" ở cỡ chữ thường, tiêu đề thẻ ở 130%) (02, 04). Cuộn xuống thì FAB thu nhỏ nên không chặn thao tác.
- Placeholder ô nhập nay ngắn ("Idol nói gì?") — hết bị cắt.

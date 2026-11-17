# Bộ asset CH Play — Idol Chat Maker

Tạo ngày 2026-11-17 từ bản `ui-upgrade` @ 05ac40e, chạy trên máy ảo Pixel 7 / Android 16. Thanh trạng thái để ở chế độ demo (9:30, pin đầy, không thông báo).

| File | Dùng cho | Kích thước | Định dạng |
|---|---|---|---|
| `icon_512.png` | Biểu tượng ứng dụng | 512×512 | PNG 32-bit (RGBA), 24 KB |
| `feature_1024x500.png` | Hình ảnh nổi bật | 1024×500 | PNG 24-bit, không alpha |
| `01_studio.png` … `07_characters.png` | Ảnh chụp màn hình điện thoại (7 ảnh, đúng thứ tự nên đăng) | 1080×1920 (9:16) | PNG 24-bit, 0,6–0,9 MB |
| `store-listing-vi.md` | Tên, mô tả ngắn, mô tả đầy đủ, ghi chú phát hành, gợi ý khai Play Console | — | Văn bản |
| `source/` | HTML + script để tạo lại ảnh khi giao diện thay đổi | — | — |

Yêu cầu CH Play mà bộ này đáp ứng:
- Icon 512×512, PNG 32-bit.
- Ảnh nổi bật 1024×500, PNG 24-bit.
- Ảnh chụp màn hình: từ 2 đến 8 ảnh, mỗi cạnh từ 320 đến 3840 px, tỉ lệ không quá 2:1. Ảnh từ 1080 px trở lên đủ điều kiện được Google Play đề xuất.
- Tên tối đa 30 ký tự: dùng 30.
- Mô tả ngắn tối đa 80 ký tự: dùng 76.
- Mô tả đầy đủ tối đa 4000 ký tự: dùng 1893.

Font chữ trong ảnh: Be Vietnam Pro, lấy từ chính app (giấy phép SIL OFL). Mọi hình ảnh đều lấy từ giao diện app hoặc icon của app, không dùng ảnh nào bên ngoài.

## ⚠ Cần xử lý trước khi nộp
Tên ba phong cách trong app đang trùng tên thương hiệu của người khác: "Lysn Bubble", "Weverse DM", "Instagram DM". Các tên này xuất hiện ở ba chỗ:
- Header Studio, nên có trong ảnh 01, 03, 04 và ảnh nổi bật.
- Thẻ mẫu ở Trang chủ (ảnh 06).
- Ngay trong ảnh xuất ra (ảnh 05).

Google Play có thể từ chối app theo chính sách Mạo danh / Sở hữu trí tuệ.

Nên đổi tên ba phong cách trong app trước khi nộp, ví dụ "Bong bóng pastel", "Neon tối", "Hoàng hôn". Sau đó tạo lại bộ ảnh: chạy lại `source/gen.py` trên ảnh chụp mới, phần ghép khung không phải làm tay lại.

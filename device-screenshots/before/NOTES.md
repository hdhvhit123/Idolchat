# Ảnh chụp thiết bị — bản `main` @ 12edf51 (trước nâng cấp)

- Thiết bị: máy ảo Pixel 7 (1080×2400, 420 dpi), Android 16 (API 36, google_apis x86_64), bàn phím ảo Gboard.
- Build: `gradlew.bat :app:assembleDebug` bằng JDK 21 (jbr-21.0.11) — thành công.
- Ngày chụp: 2026-11-16. Tất cả ảnh chụp bằng `adb exec-out screencap`.
- Lưu ý: ở các ảnh 01–18 (chế độ sáng), biểu tượng thanh trạng thái bị cắt nửa trên. Đây là lỗi
  hiển thị của máy ảo vừa khởi động; chụp lại sau đó (và app Cài đặt) thì hiển thị bình thường → **không phải lỗi app**.

## Danh sách ảnh

| Ảnh | Màn / bước |
|---|---|
| 01_home | Trang chủ lần đầu mở (có sẵn kịch bản mẫu) |
| 02_new_story_dialog | Hộp thoại "Kịch bản mới" |
| 03_new_story_keyboard | Hộp thoại khi bàn phím mở |
| 04_new_story_empty_create | Bấm "Tạo" khi để trống tên → vào Studio |
| 05_studio_typing_keyboard | Studio trống, đang gõ tin (bàn phím mở) |
| 06_studio_new_three_msgs | Đã gửi 3 tin bằng 3 người nói (Minho / Y/N / Dẫn truyện) |
| 07_home_after_create | Trang chủ sau khi tạo kịch bản |
| 08 / 09 / 10 / 10b | Kịch bản mẫu ở Lysn Bubble / Weverse DM / Instagram DM / Instagram sau khi snackbar tắt |
| 11_message_action_sheet | Bảng thao tác khi chạm 1 tin nhắn |
| 12_voice_dialog | Hộp thoại tin nhắn thoại |
| 13_characters, 14_edit_character | Dàn nhân vật, sửa nhân vật |
| 15 / 16 / 17 / 18 | Tap-to-Chat: bắt đầu / đang soạn / giữa chừng (4/9) / kết thúc |
| 19–23 | Chế độ tối hệ thống: Trang chủ, Kịch bản mới, Studio, Dàn nhân vật, Sửa nhân vật |
| 24_fontscale130_home | Trang chủ với cỡ chữ hệ thống 130% |
| 26_home_card_menu | Menu ⋮ của thẻ kịch bản (Đổi tên / Xóa) |
| 27_studio_overflow_menu | Menu ⋮ trong Studio (Xuất ảnh cuộn dài / Đổi tên / Tên trên khung chat / Quản lý nhân vật) |
| 28_export_result | Sau khi xuất: snackbar "Đã lưu ảnh vào Pictures/AU Chat Studio" + nút Chia sẻ |
| 29_exported_long_image | File PNG thật đã xuất (lấy từ máy ảo bằng adb), 1080×1920, có watermark |

## Đã kiểm chứng khi chạy

- Không crash trong toàn bộ luồng (logcat `-b crash` trống). Mở app nguội: 1,8–2,3 s (bản debug trên máy ảo, chỉ để tham khảo).
- Bàn phím: ô nhập Studio và hộp thoại "Kịch bản mới" đều được đẩy lên đúng, không bị che (03, 05).
- Đổi phong cách, gửi tin, đổi người nói, bảng thao tác, Tap-to-Chat (đang soạn → hết → Xem lại/Thoát) đều hoạt động.

## Vấn đề phát hiện (theo mức độ)

### Lỗi / hành vi sai
1. **Huy hiệu "đổi ảnh" trên avatar bị cắt** (14, 23): nút nhỏ ở góc dưới phải avatar bị clip bởi hình tròn, chỉ thấy nửa icon → khó nhận ra là nút chọn ảnh.
2. **Snackbar đổi phong cách hiện tên cũ** (10): bấm đổi phong cách 2 lần liên tiếp, giao diện đã là Instagram DM nhưng snackbar vẫn ghi "Phong cách: Weverse DM" (snackbar xếp hàng thay vì thay thế).
3. **Thanh chọn người nói bị cuộn lệch** (04, 05, 08): người đang được chọn (Minho) nằm ngoài màn hình bên trái; chip đầu tiên thấy được là "Hyunjin" bị cắt chữ. Người mới không biết đang nói với tư cách ai ngoài dòng placeholder bị cắt "Nhắn với tư cách Minho (Bi…".
4. **Thanh trượt độ dài tin nhắn thoại hiển thị sai** (12): giá trị 0:08 nhưng con trượt nằm sát mép trái, không thấy phần track đã chọn; track hồng nhạt khó nhìn.
5. Nhãn trợ năng của nút "Thêm" trong thanh người nói là "Người nói: Thêm" (đọc sai cho TalkBack).

### UX
6. **Tạo kịch bản với tên trống** (04): được phép, ra "Kịch bản chưa đặt tên" — chấp nhận được, nhưng không có cách đổi tên nhanh trong Studio ngoài menu ⋮ (chưa kiểm tra menu này).
7. **Tap-to-Chat bắt đầu bằng màn đen trống** (15): chỉ có dòng gợi ý xám nhỏ ở đáy, không có lời mời "Chạm để bắt đầu" ở giữa; dòng gợi ý tương phản thấp trên nền đen.
8. **Kết thúc Tap-to-Chat** (18): chỉ có Thoát / Xem lại; không có lối tắt "Xuất ảnh / Chia sẻ" ngay lúc người dùng hài lòng nhất. Nút "Xem lại" màu hồng pastel M3 mặc định lạc tông với phong cách Instagram.
9. Header chat có icon gọi điện / video (04–18) nhưng không làm gì → gây kỳ vọng sai (nên là trang trí rõ ràng hoặc bỏ tương tác).
10. Bảng thao tác tin nhắn (11): "Đổi người nói" thiếu lựa chọn "Dẫn truyện"; tên dài bị cắt "Minho (Bi…"; không có "Nhân bản" / "Chèn tin phía trên".
11. Trang chủ: phụ đề thẻ kịch bản là tin cuối ("Hyunjin đã xem") — dễ hiểu nhầm là trạng thái. Không có tìm kiếm/sắp xếp; danh sách sắp theo lần sửa cuối nên thứ tự nhảy sau khi chỉ đổi phong cách (07 → 19).
12. Hộp thoại sửa nhân vật: nút "Xóa" cùng màu primary với "Hủy", không phải màu cảnh báo; tiêu đề "Tùy chỉnh Nhân vật Thần tượng" cả khi sửa chính "Y/N (Bạn)".

### Giao diện
13. **Studio ở phong cách tối (Weverse/Instagram) trong chế độ sáng hệ thống** (09, 10): thanh tiêu đề và ô nhập vẫn sáng, phần chat đen → hai nửa màn hình lệch tông.
14. **Lysn Bubble: chữ trắng trên bong bóng xanh pastel `#7CB8EB`** (06, 08) — tương phản ~2:1, dưới chuẩn WCAG AA 4,5:1.
15. Cỡ chữ 130% (24): chip "Riêng tư" xuống 2 dòng, "9 tin nhắn" gãy dòng, tiêu đề thẻ bị cắt → hàng chip nên cuộn ngang hoặc tự xuống hàng gọn.
16. Trang chủ nhiều khoảng trống dưới danh sách; thẻ hero chiếm ~1/3 màn hình nhưng không có hành động.
17. Avatar chữ: "M"/"H" chữ trắng nhưng "YN" chữ đen — không nhất quán (có thể do tính tương phản tự động).
18. Chế độ tối hệ thống (19–23) nhìn chung ổn, nhất quán.

19. **Xuất ảnh — tính năng giá trị nhất nhưng bị giấu trong menu ⋮** (27): xuất chạy tốt (~1 s, file 196 KB, 29), có watermark, nhưng người mới khó tìm thấy; nên có nút Xuất/Chia sẻ nổi bật trên thanh Studio và ở màn kết thúc Tap-to-Chat.
20. Ảnh xuất (29) bỏ mất avatar nhóm và các icon header so với màn hình — ổn, nhưng header ảnh xuất ghi "Instagram DM" (tên preset) thay vì trạng thái như trong app.

### Chưa kiểm tra được
- Chọn ảnh qua Photo Picker, gửi ảnh, nút "Chia sẻ" sau khi xuất (cần thao tác với UI hệ thống).
- Đổi tên / Xóa kịch bản (chỉ mở menu, chưa thực hiện).
- Thiết bị thật, màn hình nhỏ (< 400 dp) và máy tính bảng.
- Hiệu năng trên bản release (số liệu ở đây là bản debug trên máy ảo).

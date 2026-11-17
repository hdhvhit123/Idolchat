# Kiểm tra applicationId mới — ui-upgrade @ 23f589d

- `output-metadata.json`: applicationId `io.github.hdhvhit123.idolchat`, versionName 1.0.0. Build debug thành công (JDK 21).
- `adb install` (không -r, không gỡ app cũ): cài thành app riêng. `pm list packages` có cả `io.github.hdhvhit123.idolchat` và `com.fandom.auchatstudio`.
- App cũ không bị ảnh hưởng: vẫn còn 2 kịch bản của nó.
- Dữ liệu của app cũ **không** chuyển sang app mới. Đây là hành vi đúng vì hai app khác mã định danh, nên người đang dùng bản cũ sẽ phải bắt đầu lại.

| Kiểm tra trên app mới | Kết quả | Ảnh |
|---|---|---|
| Lần mở đầu | Mở được, mất 7,9 s (lần đầu sau khi cài, bản debug trên máy ảo). Truyện mẫu "Tin nhắn lúc nửa đêm" được tạo: 9 tin, 3 nhân vật, có thẻ hướng dẫn 3 bước | 02 |
| Mở Studio | ĐẠT | 03 |
| Phát thử (Tap-to-Chat) | ĐẠT — đã xem tới 3/9 | 04 |
| Xuất ảnh | ĐẠT — file trong `Pictures/Idol Chat Maker`; MediaStore ghi chủ sở hữu là `io.github.hdhvhit123.idolchat` | — |
| Chọn avatar bằng Photo Picker | ĐẠT — file `files/images/avatar_…webp`, avatar hiện ở Dàn nhân vật | 05 |
| Crash | Không có (`logcat -b crash` trống) | — |
| Màn hình ứng dụng | Hai icon "Idol Chat" nằm cạnh nhau, giống hệt nhau, không phân biệt được | 01 |

Lưu ý thêm: truyện mẫu tự tạo dùng tên nhân vật "Minho (Bias)" và "Hyunjin", trùng tên thật của các thành viên nhóm nhạc K-pop. Đây là thứ người dùng mới thấy đầu tiên, và cũng xuất hiện trong ảnh nếu họ chụp. Nên cân nhắc đổi sang tên chung như "Idol", "Thành viên" (giống các mẫu khác) trước khi lên Play.

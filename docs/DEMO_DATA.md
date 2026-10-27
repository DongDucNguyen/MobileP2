# 50 nhân sự mẫu

Bộ dữ liệu có mã **DEMO001–DEMO050**, đủ 9 trường thông tin, 25 hồ sơ Nam và 25 hồ sơ Nữ, phân bố vào Hành chính, Phần mềm, Kỹ thuật và Dự án. Tên, năm sinh, quê quán, trình độ và lương là dữ liệu **hư cấu để minh họa**, không phải thông tin của người trong ảnh.

Ảnh chân dung được tải từ [Random User](https://randomuser.me/), xem [thông báo nguồn ảnh](https://randomuser.me/copyright). Mỗi hồ sơ ghi URL gốc và SHA-256 ảnh trong `app/src/debug/assets/demo/employees.json`. Các JPEG đã được lưu cùng dự án, chỉ đóng gói trong APK debug.

## Nạp dữ liệu vào bản debug

Sau khi cài `app-debug.apk`, chạy với `adb` trong Android SDK `platform-tools`:

```powershell
adb shell am force-stop vn.edu.nhansu
adb shell am instrument -w vn.edu.nhansu/vn.edu.nhansu.demo.DemoSeedInstrumentation
adb shell am start -n vn.edu.nhansu/.ui.MainActivity
```

Nếu kết nối nhiều máy, thêm `-s <device-id>` ngay sau `adb`.

Công cụ kiểm tra đủ 50 ảnh đọc được rồi nạp hồ sơ trong một transaction. Nó bỏ qua mã đã tồn tại, không cập nhật hay xóa hồ sơ hiện có. Kết quả trả về gồm `inserted`, `skipped_existing`, `total` và `validated_photos`. Chạy lại không tạo thêm bản sao; nếu bạn đã xóa một hồ sơ mẫu rồi nạp lại, mã mẫu còn thiếu sẽ được thêm lại.

Ảnh được sao chép vào thư mục riêng `files/demo_portraits` của ứng dụng, database lưu `file://` URI nên vẫn hiển thị khi mất mạng. Khi thay ảnh trong Detail, cơ chế chọn ảnh thiết bị vẫn hoạt động như trước.

Việc nạp mẫu chỉ chạy khi gọi lệnh trên; mở ứng dụng bình thường không tự chèn dữ liệu. Cài đè APK bằng `adb install -r` giữ database hiện có. Bản release không chứa công cụ hoặc bộ ảnh debug này.

## Chuẩn bị lại ảnh và JSON

```powershell
py -3 tools/prepare_demo_data.py
.\gradlew.bat assembleDebug
```

Script kiểm tra đúng 50 JPEG có nội dung khác nhau trước khi ghi JSON. Không cần chạy lại script khi chỉ build hoặc cài ứng dụng vì ảnh đã có trong dự án.

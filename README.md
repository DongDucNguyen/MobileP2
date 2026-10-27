# Quản lý nhân sự — Kotlin + SQLite

**B23DCVT311 - ĐÔNG ĐỨC NGUYÊN**

Dự án Android sử dụng Kotlin, XML, View Binding và `DBHelper : SQLiteOpenHelper`. Mỗi Activity đều có dòng MSSV và họ tên ở đầu màn hình. Dữ liệu được lưu cục bộ trong `nhansu.db`, không cần máy chủ.

## Mở và chạy

1. Mở **thư mục MB3** bằng Android Studio, chờ Gradle Sync hoàn tất.
2. Sử dụng **JDK 17**, Android SDK Platform **35**, Build Tools **36.0.0**. Android Studio có thể hướng dẫn cài các SDK còn thiếu.
3. Chọn máy ảo hoặc điện thoại **Android 8.0 / API 26 trở lên**, nhấn **Run app**.
4. Lần đầu, danh sách rỗng. Nhấn **+ Thêm nhân sự (Add)** để nhập hồ sơ đầu tiên.

`local.properties` chỉ cấu hình SDK của máy hiện tại và đã được bỏ qua trong Git. Trên máy khác, Android Studio sẽ tạo lại đường dẫn SDK phù hợp.

Build bằng PowerShell tại thư mục dự án:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
# Cần kết nối thiết bị/máy ảo trước khi chạy kiểm thử Android:
.\gradlew.bat connectedDebugAndroidTest
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Chức năng và bố trí nút

| Màn hình | Thao tác | Kết quả |
|---|---|---|
| Main | Nhập từ khóa + **Search** hoặc phím tìm trên bàn phím | Tìm theo mã, tên, quê quán, trình độ, phòng ban; hỗ trợ có/không dấu |
| Main | Để trống/xóa ô tìm kiếm | Hiển thị toàn bộ nhân sự |
| Main | **+ Thêm nhân sự (Add)** ở cuối | Mở `AddActivity` |
| Main | Chạm một dòng | Mở `DetailActivity`, điền đủ thông tin hiện tại |
| Main | Nhấn giữ một dòng | Hỏi xác nhận xóa: **Có (Yes)** xóa và tải lại, **Không (No)** giữ nguyên |
| Add | **Refresh** bên trái ở cuối | Xóa mọi ô nhập, lựa chọn giới tính/phòng ban và ảnh; không ghi SQLite |
| Add | **Add** bên phải ở cuối | Kiểm tra dữ liệu, thêm vào SQLite, đóng Add và cập nhật Main |
| Detail | **Cancel** bên trái ở cuối | Quay về Main, không lưu nội dung chỉnh sửa |
| Detail | **Update** bên phải ở cuối | Kiểm tra dữ liệu, cập nhật đúng hồ sơ, quay về Main |
| Add/Detail | Mũi tên quay lại hoặc nút Back hệ thống | Đóng form, không ghi thay đổi |

Form cuộn được; nhóm nút cuối màn hình luôn nằm ngoài vùng cuộn. Các vùng thanh hệ thống và bàn phím được xử lý bằng Window Insets. Sau Add/Update thành công, Main xóa bộ lọc để hồ sơ mới/cập nhật luôn xuất hiện. Cancel giữ bộ lọc cũ.

## Dữ liệu

| Cột SQLite | Kiểu | Ý nghĩa |
|---|---|---|
| `_id` | INTEGER PRIMARY KEY AUTOINCREMENT | Khóa nội bộ ổn định khi sửa mã nhân sự |
| `code` | TEXT UNIQUE COLLATE NOCASE | Mã nhân sự, không trùng kể cả khác chữ hoa/thường ASCII |
| `full_name` | TEXT | Họ và tên |
| `birth_year` | INTEGER | Năm sinh |
| `gender` | TEXT | Nam, Nữ, Khác |
| `hometown` | TEXT | Quê quán |
| `education` | TEXT | Trình độ, nhập tự do |
| `photo_uri` | TEXT | URI ảnh đã chọn; chuỗi rỗng nếu chưa có |
| `salary` | INTEGER | Lương nguyên VNĐ, dùng Kotlin `Long` để tránh sai số |
| `department` | TEXT | Hành chính, Phần mềm, Kỹ thuật, Dự án |
| `search_text` | TEXT | Nội dung chuẩn hóa phục vụ tìm kiếm không dấu |

Các trường có dấu `*` là bắt buộc, ảnh không bắt buộc. Năm sinh từ 1900 đến năm hiện tại. Lương nhập số nguyên không âm, không kèm dấu phân cách; danh sách sẽ tự định dạng VNĐ. Mã trùng được chặn ở cả form và ràng buộc UNIQUE của SQLite.

Ảnh được chọn qua `ActivityResultContracts.OpenDocument`, lưu quyền đọc URI lâu dài bằng `takePersistableUriPermission`. Ảnh gốc vẫn nằm tại nguồn người dùng chọn, không được lưu dưới dạng BLOB vào SQLite. `PhotoLoader` đọc ảnh thu nhỏ ở luồng nền, có bộ nhớ đệm; nếu ảnh nguồn bị xóa hoặc quyền bị thu hồi, ứng dụng hiển thị avatar mặc định và cho chọn lại ảnh.

Không có dữ liệu mẫu tự chèn. Đã bổ sung bộ **50 nhân sự mẫu kèm ảnh ngoại tuyến** cho bản debug; xem [cách nạp và nguồn ảnh](docs/DEMO_DATA.md). Đóng/mở ứng dụng vẫn giữ dữ liệu; gỡ ứng dụng hoặc xóa dữ liệu ứng dụng sẽ xóa database.

## Đọc mã nguồn

```text
app/src/main/
├── AndroidManifest.xml
├── java/vn/edu/nhansu/
│   ├── data/
│   │   ├── Employee.kt              # Model đầy đủ các trường nhân sự
│   │   ├── EmployeeRules.kt         # Chuẩn hóa tìm kiếm, kiểm tra năm/lương
│   │   └── DBHelper.kt              # Tạo bảng, SELECT, INSERT, UPDATE, DELETE
│   └── ui/
│       ├── BaseActivity.kt          # DBHelper, vùng thanh hệ thống/bàn phím
│       ├── MainActivity.kt          # Danh sách, tìm kiếm, xác nhận xóa
│       ├── EmployeeAdapter.kt       # Gắn dữ liệu vào từng dòng ListView
│       ├── AddActivity.kt           # Activity thêm mới
│       ├── DetailActivity.kt        # Activity chi tiết/cập nhật
│       ├── EmployeeFormActivity.kt  # Form chung, validation và sự kiện nút
│       └── PhotoLoader.kt           # Đọc ảnh thu nhỏ bất đồng bộ
└── res/
    ├── layout/activity_main.xml
    ├── layout/activity_employee_form.xml
    ├── layout/item_employee.xml
    ├── layout/view_identity.xml
    ├── layout/item_spinner.xml
    ├── drawable/                   # Nền, nút, biểu tượng vector
    └── values/                     # Chuỗi tiếng Việt, màu, styles
```

`AddActivity` và `DetailActivity` là **hai Activity riêng** và kế thừa `EmployeeFormActivity` để tránh lặp code. Chỉ khi nhấn Add/Update mới gọi hàm ghi trong `DBHelper`; Cancel/Refresh không thay đổi bản ghi. Các truy vấn dùng tham số để tránh ghép chuỗi SQL từ dữ liệu nhập.

Database hiện ở version 1. Khi đổi schema, cần tăng version và bổ sung migration trong `onUpgrade`; không xóa bảng để nâng cấp. CRUD đồng bộ được giữ đơn giản cho quy mô bài tập; nếu mở rộng lên danh sách lớn, nên chuyển truy vấn sang luồng IO và thêm phân trang.

## Kiểm thử

Kết quả đã chạy ngày **23/10/2026**: build APK thành công; **3/3 unit tests** và **5/5 Android tests** đạt trên máy ảo Pixel 9 / Android 17. Lint: **0 lỗi, 13 cảnh báo** (phiên bản thư viện/target SDK, cấu hình backup và gợi ý XML/định dạng). Các phiên bản dependency đã được cố định trong Gradle để có thể build lại.

- `EmployeeRulesTest`: tìm kiếm tiếng Việt, năm sinh, lương âm/sai định dạng/tràn số.
- `DBHelperTest`: CRUD, giữ đủ trường sau khi đóng/mở DB, tìm kiếm, mã trùng, ký tự SQL đặc biệt.
- `EmployeeFlowTest`: các nút Add/Refresh/Search/Cancel/Update, nhấn giữ và Yes/No, khôi phục bộ lọc khi Activity được tạo lại.

Kiểm thử database dùng `employee-test.db` riêng. Kiểm thử giao diện thêm hồ sơ có mã `UITEST...` và dọn các hồ sơ đó sau khi chạy, không xóa hồ sơ khác.

Kiểm tra thủ công bổ sung: chọn ảnh từ thiết bị, đổi/bỏ ảnh, mở lại ứng dụng để kiểm tra ảnh; xoay màn hình khi đang nhập; thử bàn phím và cỡ chữ lớn trên thiết bị sử dụng.

Ảnh chụp trong kiểm thử: [Main](docs/screenshots/qa/01-main.png), [Add](docs/screenshots/qa/02-add.png), [Detail](docs/screenshots/qa/03-detail.png), [xác nhận xóa](docs/screenshots/qa/04-delete.png). Hồ sơ trong các ảnh là dữ liệu kiểm thử, không được chèn sẵn vào ứng dụng.

Tài liệu Android: [SQLiteOpenHelper](https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper), [Android Gradle Plugin 8.7](https://developer.android.com/build/releases/agp-8-7-0-release-notes).

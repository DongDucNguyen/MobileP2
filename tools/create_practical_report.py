"""Tạo báo cáo PDF từ ảnh chụp thực tế bằng ReportLab; không chỉnh sửa ảnh giao diện."""
from pathlib import Path
import json
import re
import sys
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "tmp/pdf-deps"))
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, white
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph, Table, TableStyle
from PIL import Image
from pypdf import PdfReader

OUT = ROOT / "output/pdf/Bao_cao_thuc_hanh_Quan_ly_nhan_su_B23DCVT311.pdf"
SHOTS = ROOT / "docs/screenshots/report"
SRC = ROOT / "app/src/main/java/vn/edu/nhansu"
W, H = A4
M = 42
CW = W - M * 2
GREEN = HexColor("#146C60")
INK = HexColor("#172C38")
MUTED = HexColor("#566875")
PALE = HexColor("#E7F3EF")
BORDER = HexColor("#D7E2E6")
CODE_BG = HexColor("#F5F8F9")
PAGE_COUNT = 19
CODE_SIZE = 7.6
CODE_LEADING = 9.9
CODE_GUTTER = 24
# Tô màu tối giản theo từng dòng: chú thích, chuỗi và từ khóa Kotlin.
CODE_TOKEN = re.compile(r'(?P<comment>//.*$)|(?P<string>"""|"(?:[^"\\]|\\.)*")|(?P<keyword>\b(?:class|object|fun|val|var|'
                        r'override|private|protected|abstract|if|else|return|try|catch|finally|for|in|null|true|false|'
                        r'companion|const|data|with|use)\b)')
CODE_STYLE = {None: ("Consolas", INK), "comment": ("Consolas-Italic", HexColor("#6B7C88")),
              "string": ("Consolas", HexColor("#A0521B")), "keyword": ("Consolas-Bold", GREEN)}
for name, filename in [("Arial", "arial.ttf"), ("Arial-Bold", "arialbd.ttf"), ("Arial-Italic", "ariali.ttf"),
                       ("Consolas", "consola.ttf"), ("Consolas-Bold", "consolab.ttf"), ("Consolas-Italic", "consolai.ttf")]:
    pdfmetrics.registerFont(TTFont(name, str(Path("C:/Windows/Fonts") / filename)))
pdfmetrics.registerFontFamily("Arial", normal="Arial", bold="Arial-Bold", italic="Arial-Italic", boldItalic="Arial-Bold")


def code_runs(line):
    runs, pos = [], 0
    for match in CODE_TOKEN.finditer(line):
        runs += [(line[pos:match.start()], None), (match.group(), match.lastgroup)]
        pos = match.end()
    return [run for run in runs + [(line[pos:], None)] if run[0]]


def wrap_runs(runs, limit, indent):
    """Ngắt dòng dài, giữ màu của từng đoạn; dòng tiếp theo thụt thêm 4 ký tự."""
    rows, row, length = [], [], 0
    for text, kind in runs:
        while text:
            if length >= limit:
                rows.append(row)
                row, length = [(" " * (indent + 4), None)], indent + 4
            take = text[:limit - length]
            row.append((take, kind))
            length += len(take)
            text = text[len(take):]
    return rows + [row]


class Report:
    def __init__(self):
        OUT.parent.mkdir(parents=True, exist_ok=True)
        self.pdf = canvas.Canvas(str(OUT), pagesize=A4, pageCompression=1)
        self.pdf.setTitle("Báo cáo thực hành - Quản lý nhân sự Kotlin và SQLite")
        self.pdf.setAuthor("ĐÔNG ĐỨC NGUYÊN - B23DCVT311")
        self.pdf.setSubject("Triển khai ứng dụng Android và minh chứng chạy các chức năng")
        self.page = 0

    def text(self, value, x, top, width=CW, size=10.5, color=INK, bold=False, leading=None):
        style = ParagraphStyle("p", fontName="Arial-Bold" if bold else "Arial", fontSize=size,
                               leading=leading or size * 1.45, textColor=color, spaceAfter=0)
        paragraph = Paragraph(value, style)
        _, height = paragraph.wrap(width, H)
        paragraph.drawOn(self.pdf, x, H - top - height)
        return height

    def rect(self, x, top, width, height, color, radius=0):
        self.pdf.setFillColor(color)
        if radius:
            self.pdf.roundRect(x, H - top - height, width, height, radius, stroke=0, fill=1)
        else:
            self.pdf.rect(x, H - top - height, width, height, stroke=0, fill=1)

    def start(self, section, title, subtitle=""):
        if self.page:
            self.pdf.showPage()
        self.page += 1
        self.rect(0, 0, W, 5, GREEN)
        self.text("BÁO CÁO THỰC HÀNH / ANDROID", M, 23, size=8.3, color=MUTED, bold=True)
        self.text("B23DCVT311", W - M - 85, 23, width=85, size=8.3, color=GREEN, bold=True)
        self.text(section.upper(), M, 64, size=9, color=GREEN, bold=True)
        self.text(title, M, 84, size=22, bold=True, leading=28)
        if subtitle:
            self.text(subtitle, M, 124, size=10, color=MUTED)
        self.pdf.setStrokeColor(BORDER)
        self.pdf.line(M, 40, W - M, 40)
        self.text("ĐÔNG ĐỨC NGUYÊN - B23DCVT311", M, H - 29, size=8, color=MUTED)
        self.text(f"{self.page:02d} / {PAGE_COUNT}", W - M - 40, H - 29, width=40, size=8, color=MUTED)

    def box(self, title, body, top, height=70):
        self.rect(M, top, CW, height, PALE, 8)
        self.text(title, M + 14, top + 10, CW - 28, 10, GREEN, True)
        self.text(body, M + 14, top + 29, CW - 28, 10)

    def table(self, rows, widths, top, size=9, min_height=25):
        cells = [[Paragraph(escape(str(value)), ParagraphStyle("cell", fontName="Arial-Bold" if row_id == 0 else "Arial",
                  fontSize=size, leading=size * 1.4, textColor=white if row_id == 0 else INK))
                  for value in row] for row_id, row in enumerate(rows)]
        table = Table(cells, colWidths=widths, minRowHeights=[min_height] * len(rows))
        table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), GREEN),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [white, HexColor("#F4F7F8")]),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
            ("LEFTPADDING", (0, 0), (-1, -1), 9),
            ("RIGHTPADDING", (0, 0), (-1, -1), 9),
            ("TOPPADDING", (0, 0), (-1, -1), 7),
            ("BOTTOMPADDING", (0, 0), (-1, -1), 7),
            ("LINEBELOW", (0, 0), (-1, 0), 0.5, GREEN),
        ]))
        _, height = table.wrap(CW, H)
        assert top + height < H - 48, (self.page, top, height)
        table.drawOn(self.pdf, M, H - top - height)
        return height

    def photo(self, filename, x, top, height):
        path = SHOTS / f"{filename}.png"
        with Image.open(path) as image:
            width = height * image.width / image.height
        self.pdf.drawImage(str(path), x, H - top - height, width=width, height=height)
        return width

    def pair(self, section, title, intro, left, right, result):
        self.start(section, title, intro)
        image_height = 466
        image_width = image_height * 1080 / 2424
        column_width = (CW - 22) / 2
        image_top = 176
        for i, (filename, caption) in enumerate([left, right]):
            column_x = M + i * (column_width + 22)
            x = column_x + (column_width - image_width) / 2
            self.photo(filename, x, image_top, image_height)
            self.text(caption, column_x, image_top + image_height + 10, column_width, size=9.2, bold=True)
        self.box("KẾT QUẢ QUAN SÁT", result, 709, 73)

    def code(self, filename, ranges, top):
        """Trích nguyên văn mã nguồn theo số dòng; các đoạn bị lược bớt được đánh dấu // ..."""
        source = (SRC / filename).read_text(encoding="utf-8").splitlines()
        rows = []
        for i, (first, last) in enumerate(ranges):
            if i:
                rows.append((None, None))
            rows += [(number, source[number - 1].rstrip()) for number in range(first, last + 1)]
        indent = min(len(line) - len(line.lstrip()) for _, line in rows if line and line.strip())
        rows = [(number, line[indent:] if line is not None else None) for number, line in rows]
        for i, (number, line) in enumerate(rows):
            if line is None:
                following = rows[i + 1][1]
                rows[i] = (None, following[:len(following) - len(following.lstrip())] + "// ...")
        limit = int((CW - CODE_GUTTER - 18) / pdfmetrics.stringWidth("M", "Consolas", CODE_SIZE))
        lines = []
        for number, line in rows:
            parts = wrap_runs(code_runs(line), limit, len(line) - len(line.lstrip()))
            lines += [(number if j == 0 else None, part) for j, part in enumerate(parts)]
        header = 20
        height = header + 12 + len(lines) * CODE_LEADING
        assert top + height < H - 48, (self.page, filename, top, height)
        self.rect(M, top, CW, height, CODE_BG, 6)
        self.rect(M, top, CW, header, INK, 6)
        self.rect(M, top + header - 6, CW, 6, INK)
        self.pdf.setStrokeColor(BORDER)
        self.pdf.line(M + CODE_GUTTER, H - top - header, M + CODE_GUTTER, H - top - height)
        label = f"app/src/main/java/vn/edu/nhansu/{filename}  ·  dòng " + ", ".join(f"{a}–{b}" for a, b in ranges)
        self.pdf.setFillColor(white)
        self.pdf.setFont("Consolas-Bold", 8)
        self.pdf.drawString(M + 10, H - top - 13.5, label)
        baseline = top + header + 6 + CODE_SIZE
        for number, part in lines:
            if number:
                self.pdf.setFont("Consolas", CODE_SIZE - 0.6)
                self.pdf.setFillColor(MUTED)
                self.pdf.drawRightString(M + CODE_GUTTER - 5, H - baseline, str(number))
            x = M + CODE_GUTTER + 8
            for text, kind in part:
                font, color = CODE_STYLE[kind]
                self.pdf.setFont(font, CODE_SIZE)
                self.pdf.setFillColor(color)
                self.pdf.drawString(x, H - baseline, text)
                x += pdfmetrics.stringWidth(text, font, CODE_SIZE)
            baseline += CODE_LEADING
        return height

    def code_page(self, title, subtitle, blocks):
        self.start("03 / Mã nguồn", title, subtitle)
        top = 166
        for filename, ranges, note in blocks:
            top += self.code(filename, ranges, top) + 7
            top += self.text(note, M, top, size=9.2, color=MUTED) + 16
        assert top < H - 48, (self.page, top)


def create():
    capture_log = (ROOT / "docs/report-capture-result.txt").read_text(encoding="utf-16")
    if "OK (1 test)" not in capture_log:
        raise RuntimeError("Kịch bản chụp báo cáo chưa thành công; không xuất báo cáo kết quả đạt")
    required = [f"{i:02d}" for i in range(1, 20)]
    assert all(any(SHOTS.glob(prefix + "-*.png")) for prefix in required), "Thiếu ảnh chụp"
    r = Report()
    r.start("Bài thực hành", "Ứng dụng quản lý nhân sự", "Lập trình Android với Kotlin và SQLite")
    r.rect(M, 166, CW, 105, GREEN, 10)
    r.text("BÁO CÁO KẾT QUẢ THỰC HIỆN", M + 18, 183, CW - 36, 17, white, True)
    r.text("Sinh viên: ĐÔNG ĐỨC NGUYÊN<br/>Mã sinh viên: B23DCVT311<br/>Ngày thực hiện báo cáo: 23/10/2026", M + 18, 215, CW - 36, 11, white)
    r.photo("01-main", W - M - 201, 300, 450)
    r.text("MỤC TIÊU", M, 308, 250, 11, GREEN, True)
    r.text("Xây dựng ứng dụng lưu trữ và quản lý hồ sơ nhân viên trên thiết bị Android. Thực hiện tìm kiếm, thêm mới, chỉnh sửa, chọn ảnh và xóa có xác nhận.", M, 333, 248, 11)
    r.text("PHẠM VI MINH CHỨNG", M, 454, 248, 11, GREEN, True)
    r.text("19 ảnh chụp màn hình thực tế trong một kịch bản thao tác có kiểm tra kết quả.<br/><br/>Dữ liệu ban đầu: 50 nhân sự mẫu, có ảnh chân dung và đủ bốn phòng ban.", M, 480, 248, 10.5)
    r.text("MÔI TRƯỜNG CHẠY", M, 605, 248, 11, GREEN, True)
    r.text("Máy ảo Pixel 9 / Android 17<br/>Ứng dụng: vn.edu.nhansu<br/>Cơ sở dữ liệu: nhansu.db", M, 632, 248, 10.5)
    r.text("Hồ sơ mẫu là dữ liệu giả lập. Ảnh chân dung chỉ được dùng minh họa.", M, 761, CW, 8.8, MUTED)

    r.start("01 / Tổng quan", "Yêu cầu và tổ chức chương trình", "Ứng dụng có ba Activity, dùng chung lớp quản lý dữ liệu và form nhập liệu.")
    r.table([
        ["Thành phần", "Trách nhiệm"],
        ["MainActivity", "Hiển thị danh sách; tìm kiếm; mở Add/Detail; nhấn giữ để xác nhận xóa."],
        ["AddActivity", "Nhập 9 trường. Add lưu hồ sơ; Refresh xóa nội dung của form."],
        ["DetailActivity", "Hiển thị ảnh và thông tin có thể sửa. Update lưu; Cancel hủy thay đổi."],
        ["EmployeeFormActivity", "Form chung, chọn ảnh, kiểm tra dữ liệu và xử lý nút lưu."],
        ["DBHelper / Employee", "Tạo và truy vấn SQLite; biểu diễn đầy đủ thông tin một nhân sự."],
        ["EmployeeAdapter / PhotoLoader", "Hiển thị từng dòng; đọc ảnh thu nhỏ ở luồng nền và lưu bộ nhớ đệm."],
    ], [158, CW - 158], 172)
    r.text("Luồng xử lý dữ liệu", M, 484, size=13, bold=True)
    box_y = 517
    box_width = (CW - 30) / 3
    for i, (title, detail) in enumerate([("GIAO DIỆN", "Main / Add / Detail"), ("DBHELPER", "CRUD + tham số truy vấn"), ("SQLITE", "nhansu.db / employees")]):
        x = M + i * (box_width + 15)
        r.rect(x, box_y, box_width, 66, PALE, 6)
        r.text(title, x + 10, box_y + 11, box_width - 20, 10, GREEN, True)
        r.text(detail, x + 10, box_y + 33, box_width - 20, 9)
    r.text("Môi trường phát triển: Kotlin 2.0.0, Android Gradle Plugin 8.7.3, Gradle 8.9, JDK 17; compile/target SDK 35, min SDK 26. Giao diện XML sử dụng View Binding.", M, 610, size=10)
    r.box("BỐ CỤC BÁO CÁO", "Trang 3: SQLite. Trang 4-8: trích mã nguồn chính. Trang 9-18: ảnh thao tác và kết quả. Trang 19: tổng hợp kiểm tra, nhận xét và nguồn tham khảo.", 700, 76)

    r.start("02 / Cơ sở dữ liệu", "Thiết kế SQLite và xử lý nghiệp vụ", "DBHelper kế thừa SQLiteOpenHelper; bảng employees lưu hồ sơ nhân sự.")
    r.table([
        ["Cột", "Kiểu", "Ý nghĩa / ràng buộc"],
        ["_id", "INTEGER", "Khóa chính tự tăng, ổn định khi sửa mã nhân sự."],
        ["code", "TEXT", "Mã nhân sự; NOT NULL, UNIQUE, COLLATE NOCASE."],
        ["full_name", "TEXT", "Họ và tên bắt buộc, không được rỗng."],
        ["birth_year", "INTEGER", "Năm sinh; form kiểm tra 1900 đến năm hiện tại."],
        ["gender", "TEXT", "Nam, Nữ hoặc Khác."],
        ["hometown / education", "TEXT", "Quê quán và trình độ, bắt buộc trên form."],
        ["photo_uri", "TEXT", "URI ảnh; có thể rỗng khi chưa chọn ảnh."],
        ["salary", "INTEGER", "Lương VNĐ không âm; ánh xạ Kotlin Long."],
        ["department", "TEXT", "Hành chính, Phần mềm, Kỹ thuật, Dự án."],
        ["search_text", "TEXT", "Chuỗi chuẩn hóa chữ thường và bỏ dấu để tìm kiếm."],
    ], [135, 65, CW - 200], 168, size=8.8)
    r.text("Các phương thức chính", M, 560, size=13, bold=True)
    r.text("<b>getEmployees(keyword)</b> trả danh sách; <b>getEmployee(id)</b> lấy một hồ sơ.<br/><b>insertEmployee / updateEmployee / deleteEmployee</b> thực hiện CRUD.<br/><b>codeExists</b> kiểm tra trùng mã; UNIQUE bảo vệ thêm ở tầng SQLite.", M, 586, size=10)
    r.box("NGUYÊN TẮC LƯU DỮ LIỆU", "Chỉ Add/Update mới ghi hồ sơ. Cancel/Refresh không ghi thay đổi. Truy vấn dùng selectionArgs; cập nhật và xóa theo _id để tránh tác động nhầm người.", 696, 81)

    r.code_page("Mô hình dữ liệu và tạo bảng", "Employee biểu diễn một hồ sơ; DBHelper.onCreate tạo bảng employees khi mở nhansu.db lần đầu.", [
        ("data/Employee.kt", [(3, 15)],
         "Data class giữ đủ 9 thông tin của nhân sự cùng khóa nội bộ id. Lương dùng Long để lưu số nguyên VNĐ, tránh sai số của số thực."),
        ("data/DBHelper.kt", [(9, 31)],
         "Ràng buộc NOT NULL, UNIQUE và CHECK bảo vệ dữ liệu ở tầng SQLite, kể cả khi form đã kiểm tra trước. "
         "COLLATE NOCASE khiến mã “nv01” và “NV01” bị coi là trùng."),
    ])
    r.code_page("Truy vấn và các thao tác CRUD", "Mọi câu lệnh đều truyền giá trị qua selectionArgs/ContentValues, không nối chuỗi SQL.", [
        ("data/DBHelper.kt", [(39, 66)],
         "getEmployees chuẩn hóa từ khóa rồi tìm trong cột search_text bằng instr, nên ký tự %, _ hoặc dấu nháy được coi là chữ bình thường. "
         "Sửa và xóa theo _id; kết quả == 1 cho biết đúng một bản ghi bị tác động."),
        ("data/DBHelper.kt", [(68, 79)],
         "Trước khi ghi, dữ liệu được cắt khoảng trắng; search_text được tính lại từ mã, tên, quê quán, trình độ và phòng ban."),
    ])
    r.code_page("Chuẩn hóa tìm kiếm và kiểm tra dữ liệu", "EmployeeRules chứa quy tắc thuần Kotlin, có unit test riêng; form dùng các quy tắc này trước khi lưu.", [
        ("data/EmployeeRules.kt", [(7, 23)],
         "searchKey tách dấu theo chuẩn NFD, bỏ dấu thanh, đổi đ → d và gộp khoảng trắng, nhờ đó “phan mem” tìm được “Phần mềm”. "
         "parseSalary chỉ nhận chuỗi chữ số và trả null nếu tràn số."),
        ("ui/EmployeeFormActivity.kt", [(114, 122), (135, 146)],
         "readEmployee báo lỗi ngay tại ô đầu tiên còn trống hoặc sai định dạng (Hình 4), sau đó kiểm tra năm sinh, lương, giới tính và phòng ban. "
         "Chỉ khi tất cả hợp lệ mới tạo đối tượng Employee."),
    ])
    r.code_page("Form dùng chung: Add, Refresh, Cancel, Update", "AddActivity và DetailActivity là hai Activity riêng, chỉ khác giá trị isEditing.", [
        ("ui/AddActivity.kt", [(3, 6)],
         "DetailActivity khai báo tương tự với isEditing = true và hằng EXTRA_EMPLOYEE_ID để nhận _id từ Main."),
        ("ui/EmployeeFormActivity.kt", [(77, 80), (155, 181)],
         "Nút bên trái là Refresh (Add) hoặc Cancel (Detail), không gọi DBHelper. Nút bên phải kiểm tra mã trùng rồi insert/update; "
         "biến saving chặn nhấn hai lần. Thành công thì trả RESULT_OK để Main tải lại danh sách."),
    ])
    r.code_page("Màn hình Main: mở form, xóa và chọn ảnh", "Main nhận kết quả từ Add/Detail; nhấn giữ một dòng để xác nhận xóa.", [
        ("ui/MainActivity.kt", [(20, 27), (49, 57), (87, 100)],
         "Add/Update thành công thì xóa bộ lọc và tải lại danh sách. Chạm một dòng mở Detail kèm _id; nhấn giữ trả về true để không mở nhầm Detail. Chỉ nút Có (Yes) mới gọi deleteEmployee; "
         "Không (No) truyền listener null nên chỉ đóng hộp thoại."),
        ("ui/EmployeeFormActivity.kt", [(25, 36)],
         "Ảnh chọn bằng OpenDocument; takePersistableUriPermission giữ quyền đọc sau khi mở lại ứng dụng. SQLite chỉ lưu URI, không lưu ảnh dạng BLOB."),
    ])

    r.pair("04 / Danh sách và tìm kiếm", "Hiển thị danh sách và Search",
           "Mở Main; nhập “phan mem” không dấu rồi nhấn Search để lọc phòng ban Phần mềm.",
           ("01-main", "Hình 1. Danh sách ban đầu có 50 nhân sự."),
           ("02-search-department", "Hình 2. Tìm kiếm không dấu theo phòng ban."),
           "Danh sách hiển thị ảnh, tên, mã, năm sinh, giới tính, phòng ban và lương. Từ khóa không dấu vẫn trả về nhân sự thuộc phòng Phần mềm.")

    r.pair("05 / Tình huống biên", "Không có kết quả và kiểm tra bắt buộc",
           "Thử từ khóa không tồn tại; tại Add, nhấn Add khi chưa nhập thông tin.",
           ("03-search-empty", "Hình 3. Thông báo khi không có kết quả tìm kiếm."),
           ("06-validation", "Hình 4. Báo lỗi tại trường Mã nhân sự còn trống."),
           "Ứng dụng có trạng thái danh sách rỗng rõ ràng. Form chặn lưu thiếu dữ liệu và chỉ ra trường cần nhập; chưa phát sinh nhân sự mới.")

    r.pair("06 / Làm mới form", "Refresh xóa nội dung đã nhập",
           "Nhập dữ liệu thử vào form và chọn giới tính, phòng ban; sau đó nhấn Refresh.",
           ("04-before-refresh", "Hình 5. Form đang chứa dữ liệu nhập thử."),
           ("05-after-refresh", "Hình 6. Form sau thao tác Refresh."),
           "Các ô nhập trở về rỗng, lựa chọn trở về trạng thái ban đầu. Kịch bản kiểm tra các ô văn bản đã được xóa; thao tác này không thêm bản ghi vào SQLite.")

    r.pair("07 / Ảnh và thông tin cá nhân", "Chọn ảnh từ thiết bị",
           "Nhấn Chọn ảnh, chọn bao-cao-avatar.jpg trong trình chọn tệp, rồi nhập thông tin cá nhân.",
           ("07-photo-picker", "Hình 7. Trình chọn ảnh của hệ thống Android."),
           ("08-add-personal", "Hình 8. Ảnh đã chọn và thông tin cá nhân trên form."),
           "Ảnh được hiển thị xem trước. Hồ sơ dùng tên giả lập Nguyễn Minh Khoa; URI ảnh được lưu và kiểm tra đọc thành công sau khi thêm.")

    r.pair("08 / Thêm nhân sự", "Add lưu hồ sơ và cập nhật Main",
           "Nhập trình độ Đại học, phòng Phần mềm và lương 15.000.000 VNĐ; nhấn Add.",
           ("09-add-work", "Hình 9. Phần thông tin công việc trước khi lưu."),
           ("10-added-result", "Hình 10. Hồ sơ vừa thêm được tìm theo mã riêng."),
           "Add thành công, số bản ghi tăng từ 50 lên 51. Main được cập nhật; tìm theo mã hồ sơ tạm trả về đúng một nhân sự có ảnh và lương đã nhập.")

    r.start("09 / Chi tiết nhân sự", "Mở hồ sơ từ một dòng danh sách", "Chạm vào hồ sơ vừa tạo để mở DetailActivity.")
    r.photo("11-detail", M + 8, 174, 520)
    text_x = M + 258
    r.text("Dữ liệu được nạp sẵn", text_x, 190, CW - 258, 13, GREEN, True)
    r.text("Ảnh, mã nhân sự, họ tên, năm sinh, giới tính và các thông tin công việc được điền từ SQLite theo _id.", text_x, 221, CW - 258, 10.5)
    r.text("Có thể chỉnh sửa", text_x, 327, CW - 258, 13, GREEN, True)
    r.text("Các ô trên Detail vẫn cho nhập lại. Nội dung sửa chỉ nằm trên form cho đến khi nhấn Update.", text_x, 357, CW - 258, 10.5)
    r.text("Hai nút ở cuối", text_x, 459, CW - 258, 13, GREEN, True)
    r.text("<b>Cancel:</b> đóng màn hình và giữ dữ liệu cũ.<br/><br/><b>Update:</b> kiểm tra dữ liệu, cập nhật hồ sơ và quay về Main.", text_x, 489, CW - 258, 10.5)
    r.text("Hình 11. Chi tiết hồ sơ với ảnh và thông tin đã lưu.", M, 708, size=9.5, bold=True)
    r.text("Form có thể cuộn; nút Cancel/Update đặt ngoài vùng cuộn để luôn dễ thao tác.", M, 743, size=10, color=MUTED)

    r.pair("10 / Hủy chỉnh sửa", "Cancel không cập nhật dữ liệu",
           "Đổi họ tên thành “Nội dung chưa lưu”, sau đó nhấn Cancel để trở về Main.",
           ("12-before-cancel", "Hình 12. Nội dung thay đổi đang nằm trên form."),
           ("13-after-cancel", "Hình 13. Sau Cancel, họ tên cũ vẫn được giữ lại."),
           "Kiểm tra SQLite xác nhận họ tên vẫn là Nguyễn Minh Khoa. Không có thao tác update khi người dùng nhấn Cancel.")

    r.pair("11 / Cập nhật hồ sơ", "Update lưu phòng ban và mức lương mới",
           "Mở lại Detail; đổi phòng ban sang Dự án và mức lương thành 22.000.000 VNĐ.",
           ("14-before-update", "Hình 14. Thông tin công việc đã sửa, chưa lưu."),
           ("15-after-update", "Hình 15. Main hiển thị phòng Dự án và lương mới."),
           "Update giữ nguyên _id của hồ sơ, lưu đúng phòng Dự án và mức lương 22.000.000 VNĐ. Main tải lại dữ liệu sau khi lưu thành công.")

    r.pair("12 / Xác nhận xóa", "Nhấn giữ và chọn Không (No)",
           "Nhấn giữ dòng nhân sự để mở hộp thoại xác nhận; chọn Không (No).",
           ("16-delete-confirm", "Hình 16. Hộp thoại xác nhận có tên và mã hồ sơ."),
           ("17-delete-no", "Hình 17. Chọn No: nhân sự vẫn còn trong danh sách."),
           "Nhấn giữ không mở nhầm Detail. Chọn No chỉ đóng hộp thoại; bản ghi và mọi thông tin của nhân sự vẫn được giữ nguyên.")

    r.pair("13 / Xóa và hoàn tất", "Chọn Có (Yes) để xóa hồ sơ",
           "Nhấn giữ lại hồ sơ tạm, chọn Có (Yes); sau đó xóa từ khóa tìm kiếm.",
           ("18-delete-yes", "Hình 18. Sau Yes, tìm theo mã vừa xóa không có kết quả."),
           ("19-final-main", "Hình 19. Trở về danh sách 50 nhân sự mẫu ban đầu."),
           "SQLite không còn hồ sơ tạm. Danh sách trở về 50 nhân sự, chứng minh chuỗi thao tác báo cáo không làm mất dữ liệu mẫu có sẵn.")

    r.start("14 / Tổng kết", "Kết quả kiểm tra và nhận xét", "Các ảnh trong báo cáo được chụp trực tiếp từ ứng dụng trên máy ảo Pixel 9.")
    r.table([
        ["Nội dung kiểm tra", "Kết quả quan sát", "Đánh giá"],
        ["Danh sách / Search", "50 hồ sơ; tìm phòng ban không dấu; có trạng thái không kết quả.", "Đạt"],
        ["Refresh / dữ liệu bắt buộc", "Xóa ô nhập; chặn lưu form thiếu mã nhân sự.", "Đạt"],
        ["Chọn ảnh / Add", "Ảnh đọc được; bản ghi tăng lên 51; tìm đúng hồ sơ mới.", "Đạt"],
        ["Detail / Cancel", "Mở đủ thông tin; hủy sửa giữ nguyên họ tên cũ.", "Đạt"],
        ["Update", "Lưu phòng Dự án và lương 22.000.000 VNĐ.", "Đạt"],
        ["Long click / No / Yes", "No giữ bản ghi; Yes xóa đúng hồ sơ; còn 50 nhân sự.", "Đạt"],
    ], [142, CW - 194, 52], 170, size=9)
    r.text("Nhận xét", M, 467, size=13, bold=True)
    r.text("Ứng dụng đáp ứng luồng quản lý nhân sự theo yêu cầu: giao diện rõ ràng, tách lớp dữ liệu và giao diện, có xác nhận xóa và kiểm tra đầu vào. Bản build đã qua kiểm tra nền gồm 3 unit test và 5 Android test; kịch bản chụp báo cáo cũng hoàn thành thành công.", M, 493, size=10)
    r.text("Phạm vi và hướng phát triển", M, 574, size=13, bold=True)
    r.text("Minh chứng chạy trên một máy ảo; chưa thay thế kiểm thử nhiều thiết bị. Có thể mở rộng phân trang, chuyển truy vấn SQLite sang luồng IO, xuất dữ liệu và bổ sung sao lưu. Ảnh mẫu được lưu cục bộ; ảnh người dùng chọn phụ thuộc quyền đọc và tệp nguồn còn tồn tại.", M, 599, size=10)
    r.text("Nguồn tham khảo và ảnh minh họa", M, 683, size=11, bold=True)
    r.text('1. Android Developers, <link href="https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper" color="#146C60">SQLiteOpenHelper</link>.<br/>2. Ảnh chân dung: <link href="https://randomuser.me/copyright" color="#146C60">Random User - Copyright Notice</link>. Tên và thông tin nhân sự là dữ liệu giả lập.<br/>3. Mã nguồn dự án MB3; nhật ký chạy: docs/report-capture-result.txt.', M, 708, size=8.7, color=MUTED)
    assert r.page == PAGE_COUNT
    r.pdf.save()
    reader = PdfReader(str(OUT))
    assert len(reader.pages) == PAGE_COUNT
    full_text = "\n".join(page.extract_text() or "" for page in reader.pages)
    assert "ĐÔNG ĐỨC NGUYÊN" in full_text and "B23DCVT311" in full_text
    assert "\ufffd" not in full_text
    print(json.dumps({"output": str(OUT), "pages": len(reader.pages), "bytes": OUT.stat().st_size}, ensure_ascii=True))


if __name__ == "__main__":
    create()

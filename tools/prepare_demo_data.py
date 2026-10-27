"""Tạo 50 hồ sơ giả lập và tải ảnh minh họa từ Random User (chạy trong thư mục dự án)."""
import hashlib
import json
import time
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

DESTINATION = Path(__file__).resolve().parents[1] / "app/src/debug/assets/demo"
MALE_NAMES = [
    "Nguyễn Minh Anh", "Trần Quốc Bảo", "Lê Hoàng Nam", "Phạm Đức Huy", "Hoàng Gia Khánh",
    "Vũ Quang Minh", "Đặng Tuấn Kiệt", "Bùi Thành Đạt", "Đỗ Hải Đăng", "Hồ Trung Hiếu",
    "Ngô Nhật Long", "Dương Anh Tuấn", "Lý Tiến Dũng", "Đinh Văn Khang", "Mai Đức Phúc",
    "Nguyễn Thanh Tùng", "Trần Hữu Nghĩa", "Lê Công Thành", "Phạm Xuân Trường", "Hoàng Việt Hoàng",
    "Vũ Mạnh Cường", "Đặng Hồng Sơn", "Bùi Quốc Việt", "Đỗ Bảo Lâm", "Hồ Đức Thịnh",
]
FEMALE_NAMES = [
    "Nguyễn Ngọc Anh", "Trần Thu Hà", "Lê Phương Linh", "Phạm Khánh Chi", "Hoàng Thảo Nguyên",
    "Vũ Minh Châu", "Đặng Bảo Ngọc", "Bùi Thanh Hương", "Đỗ Lan Anh", "Hồ Yến Nhi",
    "Ngô Thu Trang", "Dương Hải Yến", "Lý Mỹ Duyên", "Đinh Ngọc Mai", "Mai Quỳnh Hoa",
    "Nguyễn Thanh Trúc", "Trần Diệu Linh", "Lê Tú Uyên", "Phạm Hoài An", "Hoàng Kim Ngân",
    "Vũ Bích Phương", "Đặng Ánh Dương", "Bùi Hà My", "Đỗ Như Quỳnh", "Hồ Ngọc Hân",
]
HOMETOWNS = ["Hà Nội", "Hải Phòng", "Đà Nẵng", "TP. Hồ Chí Minh", "Cần Thơ", "Huế",
             "Nghệ An", "Thanh Hóa", "Quảng Ninh", "Bắc Ninh", "Lâm Đồng", "Khánh Hòa"]
DEPARTMENTS = ["Hành chính", "Phần mềm", "Kỹ thuật", "Dự án"]
EDUCATION = ["Đại học", "Đại học", "Cao đẳng", "Thạc sĩ", "Đại học"]


def portrait(record, refresh=False):
    destination = DESTINATION / record["photo_asset"]
    destination.parent.mkdir(parents=True, exist_ok=True)
    for attempt in range(3):
        try:
            if destination.exists() and not refresh:
                data = destination.read_bytes()
            else:
                request = urllib.request.Request(record["photo_source"], headers={"User-Agent": "NhanSuDemo/1.0"})
                with urllib.request.urlopen(request, timeout=30) as response:
                    data = response.read()
            if not data.startswith(b"\xff\xd8\xff") or not data.endswith(b"\xff\xd9"):
                raise ValueError("Nguồn tải không phải ảnh JPEG hợp lệ")
            destination.write_bytes(data)
            return hashlib.sha256(data).hexdigest()
        except Exception:
            if attempt == 2:
                raise
            time.sleep(attempt + 1)


def main():
    records = []
    for index in range(50):
        person_index, gender_index = divmod(index, 2)
        gender_folder = "men" if gender_index == 0 else "women"
        # men/15 và men/17 của nguồn hiện là cùng một ảnh; thay men/17 bằng men/70.
        portrait_id = 70 if index == 14 else person_index + 10
        records.append({
            "code": f"DEMO{index + 1:03d}",
            "full_name": (MALE_NAMES if gender_index == 0 else FEMALE_NAMES)[person_index],
            "birth_year": 1978 + (index * 7 % 27),
            "gender": "Nam" if gender_index == 0 else "Nữ",
            "hometown": HOMETOWNS[index % len(HOMETOWNS)],
            "education": EDUCATION[index % len(EDUCATION)],
            "salary": 9_000_000 + (index * 7 % 60) * 500_000,
            "department": DEPARTMENTS[(index // 2 + index % 2) % 4],
            "photo_asset": f"portraits/demo{index + 1:03d}.jpg",
            "photo_source": f"https://randomuser.me/api/portraits/{gender_folder}/{portrait_id}.jpg",
        })
    # Probe before starting the parallel downloads, so a network failure stops early.
    portrait(records[0])
    portrait(records[14], refresh=True)
    with ThreadPoolExecutor(max_workers=6) as executor:
        checksums = list(executor.map(portrait, records))
    if len(set(checksums)) != 50:
        raise ValueError("Bộ ảnh có ảnh trùng; chưa xuất dữ liệu mẫu")
    for record, checksum in zip(records, checksums):
        record["photo_sha256"] = checksum
    manifest = {
        "description": "50 hồ sơ hư cấu dùng minh họa. Ảnh không đại diện danh tính, nghề nghiệp hoặc lương của người trong ảnh.",
        "source": "https://randomuser.me/",
        "source_notice": "https://randomuser.me/copyright",
        "employees": records,
    }
    (DESTINATION / "employees.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Prepared {len(records)} employees and {len(set(checksums))} distinct JPEG portraits.")
    print(f"Assets: {DESTINATION}")


if __name__ == "__main__":
    main()

# Frontend Truyện BÙM!

React 19, Vite, React Router, TanStack Query, Axios. Chạy dạng SPA, không dùng thư viện giao diện; toàn bộ CSS nằm trong `src/styles`.

## Chạy trên máy dev

Backend phải đang chạy ở cổng 8080 (xem `../backend/README.md`). Trong thư mục `frontend`:

```bash
npm install
```

```bash
npm run dev
```

Mở http://localhost:5173. Vite chuyển `/api` về backend, nên cookie đăng nhập hoạt động mà không vướng CORS.

Tài khoản mẫu: `admin@comicweb.example` / `Admin@12345` (có trang `/admin`), `minhan@comicweb.example` / `Password@123` (độc giả).

## Cấu trúc

```
src
├── api          Gọi API, tự làm mới token khi hết hạn
├── auth         Trạng thái đăng nhập
├── components   Thành phần dùng chung: Header, thẻ truyện, bình luận, bìa minh họa
├── lib          Định dạng số và ngày, chế độ sáng/tối, lịch sử đọc của khách
├── pages        Các trang độc giả
│   └── admin    Các trang quản trị, chỉ tải khi vào /admin
└── styles       base (màu, chữ), components, pages
```

## Ảnh minh họa

Dữ liệu mẫu dùng ảnh `placehold.co`. Với các ảnh này, `components/ComicArt.jsx` tự vẽ bìa và trang truyện bằng SVG. Ảnh upload thật sẽ hiển thị bình thường và không qua bước này.

## Build production

```bash
npm run build
```

Kết quả nằm trong `dist`. Khi deploy cần phục vụ frontend và API cùng domain, và trả về `index.html` cho mọi đường dẫn không phải file tĩnh để F5 không bị 404.

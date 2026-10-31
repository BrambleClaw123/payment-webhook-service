# Kết quả load test (k6)

Môi trường: 8 GB RAM, Intel Core i5-7300U, Windows, Docker Desktop. App chạy bằng `java -jar`, rate limiter tắt,
k6 chạy cùng máy với toàn bộ hệ thống. Số liệu dùng để so sánh tương đối, KHÔNG phải số liệu production.

## Webhook với event mới (mỗi request một eventId)

| Tải | Webhook đã gửi | Dropped | p95 | Trung bình | Chậm nhất | Lỗi |
|---|---|---|---|---|---|---|
| 50/s  | 1.500  | 0   | 18 ms  | 8 ms  | 84 ms  | 0 |
| 100/s | 2.859  | 142 | 242 ms | 51 ms | 1,62 s | 0 |
| 200/s | 5.920  | 81  | 254 ms | 43 ms | 837 ms | 0 |
| 400/s | 11.997 | 3   | 34 ms  | 11 ms | 131 ms | 0 |

Tổng: hơn 22.000 webhook, 0 lỗi HTTP.

## Kiểm tra độ đúng sau tải
- Event `PROCESSED`: 22479, `RECEIVED`: 0, `FAILED`: 0
- Event có nhiều hơn một khoản thanh toán: 0
- Event bị sweeper đẩy lại: 0

## Gửi trùng
1.003 request cùng một eventId trong 10 giây (100 request/giây): 0 lỗi, p95 175 ms.
Số dòng được ghi trong `payments` cho eventId đó: 1.

## Lưu ý và hạn chế
- p95 ở 100/s và 200/s cao hơn ở 400/s. Chưa xác định nguyên nhân. Các giả thuyết: JVM chưa
  được làm nóng, hàng tồn của lần chạy trước còn dây sang lần sau, k6 khởi tạo thêm VU giữa lần chạy.
  Cần chạy lại trong môi trường sạch (tắt sweeper, cấp sẵn VU, chờ xả hết hàng tồn giữa các lần).
- Sweeper có thể đẩy lại event đang xếp hàng hợp lệ nếu hàng tồn lâu hơn `min-age` (vô hại về độ đúng
  nhờ consumer idempotent, nhưng tạo thêm tải).
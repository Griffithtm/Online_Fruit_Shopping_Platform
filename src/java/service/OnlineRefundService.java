package service;

import model.*;

import config.DBConnect;
import dao.Database;
import java.sql.*;
import java.util.*;
import java.math.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * A refund is submitted once. Unknown outcomes remain pending for
 * reconciliation.
 */
public class OnlineRefundService {

    public void execute(long admin, long tid, String ip) throws Exception {
        new VnpayGateway().requireConfigured();
        Transaction refund, payment;
        try (Connection c = new DBConnect().getConnection()) {
            c.setAutoCommit(false);
            try {
                FulfillmentService.role(c, admin, "ADMIN");
                Transaction ref = Database.one(Transaction.class, c, "SELECT order_id FROM transactions WHERE id=? AND type='REFUND'", tid);
                if (ref == null) {
                    throw new IllegalArgumentException("Không tìm thấy yêu cầu hoàn tiền.");
                }
                Database.one(Order.class, c, "SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?", ref.getOrderId());
                refund = Database.one(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=? AND type='REFUND' AND status='APPROVED' AND method IN ('CARD','E_WALLET')", tid);
                if (refund == null) {
                    throw new IllegalArgumentException("Yêu cầu hoàn tiền chưa được duyệt hoặc đã được gửi. Yêu cầu đang chờ cần được đối soát.");
                }
                payment = Database.one(Transaction.class, c, "SELECT * FROM transactions WHERE id=? AND status='PAID' AND provider='VNPAY'", refund.getParentTransactionId());
                if (payment == null || payment.getProviderCreateDate() == null) {
                    throw new IllegalArgumentException("Thiếu thông tin giao dịch gốc từ cổng thanh toán.");
                }
                Database.update(c, "UPDATE transactions SET status='PENDING',updated_at=SYSUTCDATETIME() WHERE id=?", tid);
                Database.update(c, "INSERT audit_logs(actor_id,entity_type,entity_id,action) VALUES(?,'Refund',?,'SUBMITTED')", admin, tid);
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
        String tmn = System.getenv("FRESHORA_VNPAY_TMN_CODE"), secret = System.getenv("FRESHORA_VNPAY_HASH_SECRET");
        Map<String, String> p = new LinkedHashMap<>();
        p.put("vnp_RequestId", "FRREF" + tid);
        p.put("vnp_Version", "2.1.0");
        p.put("vnp_Command", "refund");
        p.put("vnp_TmnCode", tmn);
        p.put("vnp_TransactionType", (refund.getAmount()).compareTo(payment.getAmount()) == 0 ? "02" : "03");
        p.put("vnp_TxnRef", payment.getId().toString());
        p.put("vnp_Amount", (refund.getAmount()).movePointRight(2).toBigIntegerExact().toString());
        p.put("vnp_TransactionNo", payment.getGatewayRef().toString());
        p.put("vnp_TransactionDate", payment.getProviderCreateDate().toString());
        p.put("vnp_CreateBy", Long.toString(admin));
        p.put("vnp_CreateDate", DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh")).format(Instant.now()));
        p.put("vnp_IpAddr", ip);
        p.put("vnp_OrderInfo", "Freshora hoàn tiền " + tid);
        p.put("vnp_SecureHash", VnpayGateway.hash(secret, String.join("|", p.values())));
        StringJoiner json = new StringJoiner(",", "{", "}");
        p.forEach((k, v) -> json.add("\"" + k + "\":\"" + v.replace("\\", "\\\\").replace("\"", "\\\"") + "\""));
        HttpResponse<String> response = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build().send(HttpRequest.newBuilder(URI.create("https://sandbox.vnpayment.vn/merchant_webapi/api/transaction")).timeout(Duration.ofSeconds(20)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json.toString())).build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 65536) {
            throw new IllegalArgumentException("Chưa xác định được kết quả từ cổng thanh toán. Yêu cầu hoàn tiền tiếp tục chờ đối soát.");
        }
        confirm(admin, tid, response.body());
    }

    public void confirm(long admin, long tid, String signedResponse) throws Exception {
        new VnpayGateway().requireConfigured();
        if (signedResponse == null || signedResponse.length() > 65536) {
            throw new IllegalArgumentException("Phản hồi có chữ ký không hợp lệ.");
        }
        String tmn = System.getenv("FRESHORA_VNPAY_TMN_CODE"), secret = System.getenv("FRESHORA_VNPAY_HASH_SECRET");
        try (Connection c = new DBConnect().getConnection()) {
            FulfillmentService.role(c, admin, "ADMIN");
            Transaction refund = Database.one(Transaction.class, c, "SELECT * FROM transactions WHERE id=? AND type='REFUND' AND method IN ('CARD','E_WALLET') AND status IN ('PENDING','REFUNDED')", tid);
            if (refund == null) {
                throw new IllegalArgumentException("Yêu cầu hoàn tiền trực tuyến không ở trạng thái chờ.");
            }
            Transaction payment = Database.one(Transaction.class, c, "SELECT * FROM transactions WHERE id=?", refund.getParentTransactionId());
            Map<String, String> result = new HashMap<>();
            for (JsonEntry row : Database.rows(JsonEntry.class, c, "SELECT [key],value FROM OPENJSON(?)", signedResponse)) {
                String key = row.getKey();
                if (result.put(key, row.getValue()) != null) {
                    throw new IllegalArgumentException("Kết quả từ cổng thanh toán không hợp lệ.");
                }
            }
            String[] fields = {"vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message", "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode", "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType", "vnp_TransactionStatus", "vnp_OrderInfo"};
            StringJoiner signed = new StringJoiner("|");
            for (String f : fields) {
                signed.add(result.getOrDefault(f, ""));
            }
            String signature = result.get("vnp_SecureHash");
            if (signature == null || !java.security.MessageDigest.isEqual(VnpayGateway.hash(secret, signed.toString()).getBytes(java.nio.charset.StandardCharsets.US_ASCII), signature.toLowerCase(Locale.ROOT).getBytes(java.nio.charset.StandardCharsets.US_ASCII)) || !tmn.equals(result.get("vnp_TmnCode")) || !"refund".equals(result.get("vnp_Command")) || !payment.getId().toString().equals(result.get("vnp_TxnRef")) || !(refund.getAmount()).movePointRight(2).toBigIntegerExact().toString().equals(result.get("vnp_Amount")) || !Set.of("02", "03").contains(result.getOrDefault("vnp_TransactionType", "")) || !("Freshora hoàn tiền " + tid).equals(result.get("vnp_OrderInfo"))) {
                throw new IllegalArgumentException("Không xác minh được kết quả từ cổng thanh toán. Yêu cầu hoàn tiền tiếp tục chờ xử lý.");
            }
            if (!"00".equals(result.get("vnp_ResponseCode")) || !"00".equals(result.get("vnp_TransactionStatus"))) {
                throw new IllegalArgumentException("VNPay chưa xác nhận hoàn tất. Yêu cầu hoàn tiền tiếp tục chờ xử lý.");
            }
            String gateway = result.get("vnp_TransactionNo");
            if (gateway == null || !gateway.matches("[0-9]{1,15}")) {
                throw new IllegalArgumentException("Thiếu mã tham chiếu hoàn tiền từ cổng thanh toán.");
            }
            c.setAutoCommit(false);
            try {
                Database.one(Order.class, c, "SELECT id FROM orders WITH(UPDLOCK,HOLDLOCK) WHERE id=?", refund.getOrderId());
                Transaction locked = Database.one(Transaction.class, c, "SELECT * FROM transactions WITH(UPDLOCK,HOLDLOCK) WHERE id=?", tid);
                if ("PENDING".equals(locked.getStatus())) {
                    FinanceService.complete(c, locked, null, gateway);
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }
}

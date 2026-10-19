package util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * System messages from SRS III.2 (Freshora_Messages.md). Placeholders such as
 * {field_name} are filled in order of appearance by {@link #get(String, Object...)}.
 */
public final class Messages {

    public static final String MSG01 = "MSG01", MSG02 = "MSG02", MSG03 = "MSG03", MSG04 = "MSG04",
            MSG05 = "MSG05", MSG06 = "MSG06", MSG07 = "MSG07", MSG08 = "MSG08", MSG09 = "MSG09",
            MSG10 = "MSG10", MSG11 = "MSG11", MSG12 = "MSG12", MSG13 = "MSG13", MSG14 = "MSG14",
            MSG15 = "MSG15", MSG16 = "MSG16", MSG17 = "MSG17", MSG18 = "MSG18", MSG19 = "MSG19",
            MSG32 = "MSG32", MSG33 = "MSG33", MSG34 = "MSG34", MSG35 = "MSG35", MSG36 = "MSG36",
            MSG37 = "MSG37", MSG38 = "MSG38", MSG39 = "MSG39", MSG40 = "MSG40",
            // MSG81–82: added by FE-01.2 (BA-04); to be added to SRS III.2.
            MSG81 = "MSG81", MSG82 = "MSG82";

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{[a-z_]+\\}");
    private static final Map<String, String> TEXT = new HashMap<>();

    static {
        TEXT.put("MSG01", "Không có kết quả phù hợp. Vui lòng thử điều kiện tìm kiếm khác.");
        TEXT.put("MSG02", "{field_name} là bắt buộc.");
        TEXT.put("MSG03", "{field_name} không được vượt quá {max_length} ký tự.");
        TEXT.put("MSG04", "Vui lòng nhập {field_name} hợp lệ.");
        TEXT.put("MSG05", "{record_type} đã được lưu thành công.");
        TEXT.put("MSG06", "Bạn có chắc muốn {action} {record_name} không?");
        TEXT.put("MSG07", "Dữ liệu đã thay đổi. Vui lòng tải lại trang và thử lại.");
        TEXT.put("MSG08", "Không thể hoàn tất yêu cầu. Vui lòng thử lại sau.");
        TEXT.put("MSG09", "Thư điện tử hoặc mật khẩu không đúng.");
        TEXT.put("MSG10", "Tài khoản của bạn đã bị đình chỉ. Vui lòng liên hệ quản trị viên.");
        TEXT.put("MSG11", "Vui lòng đăng nhập để tiếp tục.");
        TEXT.put("MSG12", "Bạn không có quyền thực hiện thao tác này.");
        TEXT.put("MSG13", "Địa chỉ thư điện tử này đã được đăng ký. Vui lòng đăng nhập hoặc đặt lại mật khẩu.");
        TEXT.put("MSG14", "Tài khoản đã được tạo. Bạn có thể đăng nhập.");
        TEXT.put("MSG15", "Nếu địa chỉ thư điện tử đã được đăng ký, hướng dẫn đặt lại mật khẩu sẽ được gửi đến.");
        TEXT.put("MSG16", "Mật khẩu xác nhận không khớp.");
        TEXT.put("MSG17", "Mật khẩu của bạn đã được cập nhật.");
        TEXT.put("MSG18", "Địa chỉ giao hàng mặc định đã được cập nhật.");
        TEXT.put("MSG19", "Địa chỉ này đang được sử dụng bởi đơn hàng đang xử lý nên không thể sửa hoặc xóa.");
        TEXT.put("MSG20", "Bạn đã có cửa hàng hoặc yêu cầu đang xử lý.");
        TEXT.put("MSG21", "Yêu cầu đăng ký người bán đã được gửi để xét duyệt.");
        TEXT.put("MSG22", "Yêu cầu đăng ký người bán đã được duyệt. Bạn có thể bắt đầu bán hàng.");
        TEXT.put("MSG23", "Yêu cầu đăng ký người bán bị từ chối. Lý do: {reason}.");
        TEXT.put("MSG24", "Cửa hàng đã đạt giới hạn {listing_limit} tin đăng.");
        TEXT.put("MSG25", "Giá biến thể phải lớn hơn không.");
        TEXT.put("MSG26", "Ngày thu hoạch không được sau ngày nhập; ngày hết hạn phải sau ngày nhập.");
        TEXT.put("MSG27", "Đã ghi nhận lô {batch_number} và cập nhật tồn kho.");
        TEXT.put("MSG28", "Tin đăng {listing_title} đã được gửi để xét duyệt.");
        TEXT.put("MSG29", "Tin đăng {listing_title} đã được duyệt. Sản phẩm hiển thị khi cửa hàng hoạt động và còn hàng.");
        TEXT.put("MSG30", "Tin đăng {listing_title} bị từ chối. Lý do: {reason}. Vui lòng sửa trước khi gửi lại.");
        TEXT.put("MSG31", "Vui lòng nhập lý do từ chối.");
        TEXT.put("MSG32", "Biến thể sản phẩm không còn khả dụng. Vui lòng xóa hoặc chọn biến thể khác.");
        TEXT.put("MSG33", "Chỉ còn {available_quantity} {unit} của {variant_name}.");
        TEXT.put("MSG34", "Giỏ hàng đã thay đổi. Vui lòng kiểm tra giá và tồn kho trước khi xác nhận.");
        TEXT.put("MSG35", "Giỏ hàng đang trống. Vui lòng thêm sản phẩm trước khi thanh toán.");
        TEXT.put("MSG36", "Chưa hỗ trợ giao hàng đến địa chỉ này. Vui lòng chọn địa chỉ khác.");
        TEXT.put("MSG37", "Không thể áp dụng mã giảm giá: {reason}.");
        TEXT.put("MSG38", "Bạn có thể sử dụng tối đa {maximum_points} điểm cho đơn hàng này.");
        TEXT.put("MSG39", "Đơn hàng {order_code} đã được xác nhận. Thanh toán {amount} khi nhận hàng.");
        TEXT.put("MSG40", "Đơn hàng {order_code} đang chờ thanh toán. Vui lòng thanh toán trước {payment_deadline}.");
        TEXT.put("MSG41", "Đã xác nhận khoản thanh toán {amount} cho đơn hàng {order_code}.");
        TEXT.put("MSG42", "Thanh toán thất bại. Bạn có thể thử lại trước {payment_deadline}.");
        TEXT.put("MSG43", "Khoản thanh toán đang được xác minh. Vui lòng kiểm tra trạng thái trước khi thanh toán lại.");
        TEXT.put("MSG44", "Đơn hàng {order_code} đã hết hạn thanh toán. Tồn kho đã giữ được giải phóng.");
        TEXT.put("MSG45", "Đã nhận tiền sau khi đơn hàng {order_code} hết hạn. Yêu cầu hoàn tiền đã được tạo; xem trạng thái trong chi tiết đơn hàng.");
        TEXT.put("MSG46", "Không thể hủy đơn của cửa hàng theo chính sách hiện tại.");
        TEXT.put("MSG47", "Đơn hàng cửa hàng {seller_order_code} bị từ chối. Lý do: {reason}. Khoản đã thanh toán sẽ được xử lý hoàn tiền.");
        TEXT.put("MSG48", "Đơn hàng cửa hàng {seller_order_code} đã được hủy.");
        TEXT.put("MSG49", "Đơn hàng cửa hàng {seller_order_code} hiện ở trạng thái {order_status}.");
        TEXT.put("MSG50", "Không thể yêu cầu hoàn tiền: {reason}. Số tiền còn được hoàn: {refundable_amount}.");
        TEXT.put("MSG51", "Yêu cầu hoàn tiền {refund_reference} đã được gửi để xét duyệt.");
        TEXT.put("MSG52", "Yêu cầu hoàn tiền {refund_reference} đã được duyệt và đang chờ xử lý.");
        TEXT.put("MSG53", "Yêu cầu hoàn tiền {refund_reference} bị từ chối. Lý do: {reason}.");
        TEXT.put("MSG54", "Đã hoàn {amount} cho đơn hàng cửa hàng {seller_order_code}.");
        TEXT.put("MSG55", "Khoản hoàn tiền {refund_reference} đang chờ xử lý. Bạn sẽ được thông báo khi trạng thái thay đổi.");
        TEXT.put("MSG56", "Đơn hàng cửa hàng này đã có tranh chấp đang xử lý.");
        TEXT.put("MSG57", "Tranh chấp đơn hàng cửa hàng {seller_order_code}: {dispute_status}. {decision_note}");
        TEXT.put("MSG58", "Đơn giao hàng {seller_order_code} đã được giao cho bạn. Vui lòng xem hướng dẫn lấy hàng và bảo quản lạnh.");
        TEXT.put("MSG59", "Không thể phân công nhân viên này. Vui lòng chọn nhân viên giao hàng đủ điều kiện.");
        TEXT.put("MSG60", "Vui lòng cung cấp bằng chứng giao hàng và ghi nhận tiền thu hộ trước khi xác nhận giao hàng.");
        TEXT.put("MSG61", "Đơn hàng cửa hàng {seller_order_code} đã giao thành công.");
        TEXT.put("MSG62", "Vui lòng nhập lý do giao hàng thất bại.");
        TEXT.put("MSG63", "Giao đơn hàng cửa hàng {seller_order_code} thất bại. Lý do: {reason}.");
        TEXT.put("MSG64", "Bạn chỉ được đánh giá mỗi sản phẩm đã mua một lần sau khi giao thành công.");
        TEXT.put("MSG65", "Đánh giá của bạn đã được đăng.");
        TEXT.put("MSG66", "Báo cáo đã được gửi để quản trị viên xem xét.");
        TEXT.put("MSG67", "Bạn đã báo cáo đánh giá này.");
        TEXT.put("MSG68", "Đã kiểm duyệt đánh giá. Kết quả: {moderation_result}.");
        TEXT.put("MSG69", "Không thể hoàn tất đăng ký: {reason}.");
        TEXT.put("MSG70", "Đăng ký định kỳ {subscription_reference} hiện ở trạng thái {subscription_status}.");
        TEXT.put("MSG71", "Đơn hàng {order_code} đã được tạo từ đăng ký định kỳ. Vui lòng kiểm tra thanh toán và giao hàng.");
        TEXT.put("MSG72", "Không thể tạo đơn định kỳ do hết hàng. Ngày dự kiến tiếp theo: {next_date}.");
        TEXT.put("MSG73", "Đăng ký định kỳ đã tạm dừng vì gói hoặc cửa hàng không khả dụng.");
        TEXT.put("MSG74", "Điểm thưởng thay đổi {points_change} điểm: {reason}. Số dư khả dụng: {points_balance}.");
        TEXT.put("MSG75", "Hạng {tier_type} hiện là {tier_name}. Vui lòng xem quyền lợi và giới hạn mới.");
        TEXT.put("MSG76", "Cảnh báo tồn kho cửa hàng: {alert_summary}. Vui lòng kiểm tra các lô hàng.");
        TEXT.put("MSG77", "Bảng quyết toán kỳ {period} đã sẵn sàng. Số tiền thực nhận: {net_amount}.");
        TEXT.put("MSG78", "Đơn hàng cửa hàng {seller_order_code} chưa được quyết toán. Vui lòng kiểm tra vấn đề đối soát.");
        TEXT.put("MSG79", "Đã ghi nhận điều chỉnh {transaction_reference} cùng lý do và nhật ký.");
        TEXT.put("MSG80", "Không thể gửi thư thông báo {notification_reference} sau số lần thử lại tối đa. Thông báo trong hệ thống vẫn còn.");
        // Not yet in SRS III.2 (to be added): address deletion feedback, FE-01.2 / BA-04.
        TEXT.put("MSG81", "Đã xóa địa chỉ thành công.");
        TEXT.put("MSG82", "Đã xóa địa chỉ. {address} hiện là địa chỉ giao hàng mặc định của bạn.");
    }

    private Messages() {
    }

    /** Returns the message text, replacing each {placeholder} with the next argument. */
    public static String get(String code, Object... args) {
        String text = TEXT.get(code);
        if (text == null) {
            throw new IllegalStateException("Unknown message code: " + code);
        }
        Matcher m = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (m.find()) {
            String value = i < args.length ? String.valueOf(args[i++]) : m.group();
            m.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        m.appendTail(out);
        String result = out.toString();
        return Character.toUpperCase(result.charAt(0)) + result.substring(1);
    }

    /** Shortcut for a user-facing validation error. */
    public static IllegalArgumentException error(String code, Object... args) {
        return new IllegalArgumentException(get(code, args));
    }
}

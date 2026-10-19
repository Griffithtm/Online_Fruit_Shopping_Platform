package util;

/** Display codes shared by customer, seller, delivery and admin screens and notifications. */
public final class OrderCodes {

    private OrderCodes() {
    }

    /** Seller order code "&lt;order_code&gt;-&lt;store_id&gt;", e.g. FR123-7 (one SellerOrder per store per Order). */
    public static String sellerOrderCode(String orderCode, Long storeId) {
        return orderCode + "-" + storeId;
    }
}

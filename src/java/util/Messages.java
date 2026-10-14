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
        TEXT.put("MSG01", "No matching results. Try different search criteria.");
        TEXT.put("MSG02", "{field_name} is required.");
        TEXT.put("MSG03", "{field_name} must not exceed {max_length} characters.");
        TEXT.put("MSG04", "Enter a valid {field_name}.");
        TEXT.put("MSG05", "{record_type} saved successfully.");
        TEXT.put("MSG06", "Are you sure you want to {action} {record_name}?");
        TEXT.put("MSG07", "This record has changed. Refresh the page and try again.");
        TEXT.put("MSG08", "We could not complete your request. Please try again later.");
        TEXT.put("MSG09", "Incorrect email or password.");
        TEXT.put("MSG10", "Your account is suspended. Contact the administrator for assistance.");
        TEXT.put("MSG11", "Please sign in to continue.");
        TEXT.put("MSG12", "You do not have permission to perform this action.");
        TEXT.put("MSG13", "This email is already registered. Sign in or reset your password.");
        TEXT.put("MSG14", "Your account has been created. You can now sign in.");
        TEXT.put("MSG15", "If this email is registered, password reset instructions will be sent.");
        TEXT.put("MSG16", "The passwords do not match.");
        TEXT.put("MSG17", "Your password has been updated.");
        TEXT.put("MSG18", "Your default delivery address has been updated.");
        TEXT.put("MSG19", "This address is used by an open order and cannot be changed or deleted.");
        TEXT.put("MSG20", "You already have a store or an application in progress.");
        TEXT.put("MSG21", "Your seller application has been submitted for review.");
        TEXT.put("MSG22", "Your seller application is approved. Selling access is now active.");
        TEXT.put("MSG23", "Your seller application was rejected. Reason: {reason}.");
        TEXT.put("MSG24", "Your store has reached its listing limit of {listing_limit}.");
        TEXT.put("MSG25", "The variant price must be greater than zero.");
        TEXT.put("MSG26", "Harvest date must be on or before receipt date, and expiry date must be after receipt date.");
        TEXT.put("MSG27", "Batch {batch_number} has been recorded and stock updated.");
        TEXT.put("MSG28", "Listing {listing_title} has been submitted for review.");
        TEXT.put("MSG29", "Listing {listing_title} is approved. It will be visible when your store is active and stock is available.");
        TEXT.put("MSG30", "Listing {listing_title} was rejected. Reason: {reason}. Revise it before resubmitting.");
        TEXT.put("MSG31", "Enter a rejection reason.");
        TEXT.put("MSG32", "This product option is unavailable. Remove it or choose another option.");
        TEXT.put("MSG33", "Only {available_quantity} {unit} of {variant_name} are available.");
        TEXT.put("MSG34", "Your cart has changed. Review the updated prices and availability before confirming.");
        TEXT.put("MSG35", "Your cart is empty. Add a product before checkout.");
        TEXT.put("MSG36", "Delivery is not available at this address. Choose another address.");
        TEXT.put("MSG37", "This voucher cannot be applied: {reason}.");
        TEXT.put("MSG38", "You can use up to {maximum_points} points for this order.");
        TEXT.put("MSG39", "Order {order_code} is confirmed. Pay {amount} on delivery.");
        TEXT.put("MSG40", "Order {order_code} is awaiting payment. Complete payment before {payment_deadline}.");
        TEXT.put("MSG41", "Payment of {amount} for order {order_code} is confirmed.");
        TEXT.put("MSG42", "Payment failed. You may try again before {payment_deadline}.");
        TEXT.put("MSG43", "Payment is being verified. Check the order status before making another payment.");
        TEXT.put("MSG44", "Order {order_code} expired because payment was not completed in time. Reserved stock has been released.");
        TEXT.put("MSG45", "Payment was received after order {order_code} expired. A refund has been initiated; check its status in your order details.");
        TEXT.put("MSG46", "This seller order can no longer be cancelled under the current cancellation policy.");
        TEXT.put("MSG47", "Seller order {seller_order_code} was rejected. Reason: {reason}. Any paid amount will enter refund processing.");
        TEXT.put("MSG48", "Seller order {seller_order_code} has been cancelled.");
        TEXT.put("MSG49", "Seller order {seller_order_code} is now {order_status}.");
        TEXT.put("MSG50", "The refund cannot be requested: {reason}. Remaining refundable amount: {refundable_amount}.");
        TEXT.put("MSG51", "Refund request {refund_reference} has been submitted for review.");
        TEXT.put("MSG52", "Refund request {refund_reference} is approved and awaiting processing.");
        TEXT.put("MSG53", "Refund request {refund_reference} was rejected. Reason: {reason}.");
        TEXT.put("MSG54", "A refund of {amount} for seller order {seller_order_code} has been completed.");
        TEXT.put("MSG55", "Refund {refund_reference} is pending. You will be notified when its status changes.");
        TEXT.put("MSG56", "An open dispute already exists for this seller order.");
        TEXT.put("MSG57", "Dispute for seller order {seller_order_code}: {dispute_status}. {decision_note}");
        TEXT.put("MSG58", "Delivery {seller_order_code} has been assigned to you. Review the pickup and cold-chain instructions.");
        TEXT.put("MSG59", "This delivery staff member cannot be assigned to the order. Select an eligible staff member.");
        TEXT.put("MSG60", "Provide the required delivery evidence and record any COD collection before confirming delivery.");
        TEXT.put("MSG61", "Seller order {seller_order_code} has been delivered.");
        TEXT.put("MSG62", "Enter the reason for delivery failure.");
        TEXT.put("MSG63", "Delivery of seller order {seller_order_code} failed. Reason: {reason}.");
        TEXT.put("MSG64", "You may review each purchased order item once, after it has been delivered.");
        TEXT.put("MSG65", "Your review has been published.");
        TEXT.put("MSG66", "Your report has been submitted for administrator review.");
        TEXT.put("MSG67", "You have already reported this review.");
        TEXT.put("MSG68", "Review moderation completed. Result: {moderation_result}.");
        TEXT.put("MSG69", "Enrollment cannot be completed: {reason}.");
        TEXT.put("MSG70", "Subscription {subscription_reference} is now {subscription_status}.");
        TEXT.put("MSG71", "Order {order_code} has been created from your subscription. Review its payment and delivery details.");
        TEXT.put("MSG72", "Your scheduled subscription order could not be created because stock is unavailable. The next scheduled date is {next_date}.");
        TEXT.put("MSG73", "Your subscription has been paused because the plan or store is unavailable.");
        TEXT.put("MSG74", "Your points balance changed by {points_change} points: {reason}. Available balance: {points_balance}.");
        TEXT.put("MSG75", "Your {tier_type} tier is now {tier_name}. Review your current benefits and limits.");
        TEXT.put("MSG76", "Stock alert for your store: {alert_summary}. Review your harvest batches.");
        TEXT.put("MSG77", "Settlement summary for {period} is ready. Net amount: {net_amount}.");
        TEXT.put("MSG78", "Settlement remains Unsettled for seller order {seller_order_code}. Review the reconciliation exception.");
        TEXT.put("MSG79", "Adjustment {transaction_reference} has been recorded with its reason and audit history.");
        TEXT.put("MSG80", "Email notification {notification_reference} failed after the retry limit. The in-app notification remains available.");
        // Not yet in SRS III.2 (to be added): address deletion feedback, FE-01.2 / BA-04.
        TEXT.put("MSG81", "Address deleted successfully.");
        TEXT.put("MSG82", "Address deleted. {address} is now your default delivery address.");
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

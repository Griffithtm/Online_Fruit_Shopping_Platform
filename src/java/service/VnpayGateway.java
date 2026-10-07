package service;
import java.util.*;import java.net.URLEncoder;import java.nio.charset.StandardCharsets;import javax.crypto.Mac;import javax.crypto.spec.SecretKeySpec;import java.security.MessageDigest;import java.time.*;import java.time.format.DateTimeFormatter;import java.math.BigDecimal;
/** VNPay API 2.1.0 sandbox. Browser return never confirms payment. */
public class VnpayGateway {
 private final String tmn,secret,publicUrl;
 public VnpayGateway(){this(System.getenv("FRESHORA_VNPAY_TMN_CODE"),System.getenv("FRESHORA_VNPAY_HASH_SECRET"),System.getenv("FRESHORA_PUBLIC_URL"));}
 public VnpayGateway(String tmn,String secret,String publicUrl){this.tmn=tmn;this.secret=secret;this.publicUrl=publicUrl;}
 public void requireConfigured(){if(tmn==null || tmn.isBlank() || secret==null || secret.isBlank() || publicUrl==null || !publicUrl.startsWith("https://"))throw new IllegalArgumentException("VNPay sandbox is not configured. Please use COD or contact Admin.");}
 public static String hash(String secret,String data)throws Exception{Mac mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));}
 public static String canonical(Map<String,String> input){SortedMap<String,String> sorted=new TreeMap<>(input);sorted.remove("vnp_SecureHash");sorted.remove("vnp_SecureHashType");StringJoiner join=new StringJoiner("&");sorted.forEach((k,v)->{if(v!=null && !v.isEmpty())join.add(URLEncoder.encode(k,StandardCharsets.US_ASCII)+"="+URLEncoder.encode(v,StandardCharsets.US_ASCII));});return join.toString();}
 public boolean verify(Map<String,String> input)throws Exception{requireConfigured();String supplied=input.get("vnp_SecureHash");return supplied!=null && supplied.matches("[0-9a-fA-F]{128}") && tmn.equals(input.get("vnp_TmnCode")) && MessageDigest.isEqual(hash(secret,canonical(input)).getBytes(StandardCharsets.US_ASCII),supplied.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII));}
 public String payment(long ref,BigDecimal amount,String method,String ip,java.sql.Timestamp expires,String created)throws Exception{
  requireConfigured();Map<String,String> p=new TreeMap<>();DateTimeFormatter f=DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
  p.put("vnp_Version","2.1.0");p.put("vnp_Command","pay");p.put("vnp_TmnCode",tmn);p.put("vnp_Amount",amount.movePointRight(2).toBigIntegerExact().toString());p.put("vnp_CurrCode","VND");p.put("vnp_TxnRef",Long.toString(ref));p.put("vnp_OrderInfo","Freshora payment "+ref);p.put("vnp_OrderType","other");p.put("vnp_Locale","en");p.put("vnp_ReturnUrl",publicUrl.replaceAll("/$","")+"/payment/return");p.put("vnp_IpAddr",ip);p.put("vnp_CreateDate",created);p.put("vnp_ExpireDate",f.format(expires.toLocalDateTime().toInstant(ZoneOffset.UTC)));
  if(method.equals("E_WALLET"))p.put("vnp_BankCode","VNPAYQR");else p.put("vnp_BankCode","INTCARD");
  String data=canonical(p);return "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"+data+"&vnp_SecureHash="+hash(secret,data);
 }
}

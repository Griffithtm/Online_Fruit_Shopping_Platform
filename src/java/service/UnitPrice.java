package service;

import model.*;

import java.math.*;
import java.util.*;
import java.util.regex.*;

/** Converts explicitly stated pack weights only; never guesses a missing weight. */
public final class UnitPrice {
    private static final Pattern WEIGHT = Pattern.compile("(?i)(?<![\\d.,])([0-9]+(?:[.,][0-9]+)?)\\s*(kg|g)\\b");
    public static BigDecimal perKg(String pack, BigDecimal price) {
        if (pack == null || pack.matches("(?i).*\\d\\s*[x×]\\s*\\d.*")) return null;
        Matcher m = WEIGHT.matcher(pack);
        if (!m.find()) return null;
        BigDecimal weight = new BigDecimal(m.group(1).replace(',', '.'));
        if (m.group(2).equalsIgnoreCase("g")) weight = weight.movePointLeft(3);
        if (m.find() || weight.signum() <= 0) return null;
        return price.divide(weight, 2, RoundingMode.HALF_UP);
    }
    public static void decorate(List<ProductOffer> products) {
        for (ProductOffer p : products) {
            p.setUnitPriceKg(perKg(p.getOptionName(), p.getPrice()));
        }
    }
}

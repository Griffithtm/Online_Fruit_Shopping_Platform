package dao;

import config.DBConnect;
import model.PriceVariant;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PriceDAO {

    public List<PriceVariant> findBySeller(String email) throws SQLException {
        String sql = "SELECT v.VariantID, p.ProductName, v.VariantName, v.SellingPrice "
                + "FROM ProductVariants v JOIN Products p ON p.ProductID=v.ProductID "
                + "JOIN Shops s ON s.ShopID=p.ShopID JOIN Users u ON u.UserID=s.OwnerUserID "
                + "WHERE u.Email=? AND u.IsActive=1 AND s.Status='APPROVED' AND v.IsActive=1 "
                + "ORDER BY v.VariantID";
        List<PriceVariant> result = new ArrayList<>();
        try (Connection con = new DBConnect().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new PriceVariant(rs.getInt("VariantID"), rs.getString("ProductName"),
                            rs.getString("VariantName"), rs.getBigDecimal("SellingPrice")));
                }
            }
        }
        return result;
    }

    public boolean updatePrice(int variantId, BigDecimal price, String sellerEmail) throws SQLException {
        // Ownership is checked in SQL, even if someone changes the hidden form ID.
        String sql = "UPDATE v SET SellingPrice=? FROM ProductVariants v "
                + "JOIN Products p ON p.ProductID=v.ProductID "
                + "JOIN Shops s ON s.ShopID=p.ShopID JOIN Users u ON u.UserID=s.OwnerUserID "
                + "WHERE v.VariantID=? AND u.Email=? AND u.IsActive=1 "
                + "AND s.Status='APPROVED' AND v.IsActive=1";
        try (Connection con = new DBConnect().getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, price);
            ps.setInt(2, variantId);
            ps.setString(3, sellerEmail);
            return ps.executeUpdate() == 1;
        }
    }
}

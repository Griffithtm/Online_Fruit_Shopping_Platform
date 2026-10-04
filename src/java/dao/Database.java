package dao;

import java.sql.*;
import java.util.*;


public final class Database {
    private Database() {}
    public static List<Map<String,Object>> rows(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s,args);
            try (ResultSet r=s.executeQuery()) {
                List<Map<String,Object>> result=new ArrayList<>();
                while(r.next()) {
                    Map<String,Object> row=new LinkedHashMap<>();
                    for(int i=1;i<=r.getMetaData().getColumnCount();i++) row.put(r.getMetaData().getColumnLabel(i),r.getObject(i));
                    result.add(row);
                }
                return result;
            }
        }
    }
    public static Map<String,Object> one(Connection c,String sql,Object... args) throws SQLException {
        List<Map<String,Object>> r=rows(c,sql,args); return r.isEmpty()?null:r.get(0);
    }
    public static int update(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql)){bind(s,args);return s.executeUpdate();}
    }
    public static long insert(Connection c,String sql,Object... args) throws SQLException {
        try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            bind(s,args);s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}
        }
    }
    private static void bind(PreparedStatement s,Object[] args) throws SQLException {
        for(int i=0;i<args.length;i++)s.setObject(i+1,args[i]);
    }
}

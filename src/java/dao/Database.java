package dao;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

/** JDBC queries return explicit JavaBeans; SQL columns map to camelCase properties. */
public final class Database {
    private Database() {}

    private static String key(String name) {
        return name.replace("_", "").toLowerCase(Locale.ROOT);
    }

    private static final ClassValue<Map<String, Method>> SETTERS = new ClassValue<>() {
        @Override protected Map<String, Method> computeValue(Class<?> type) {
            try {
                Map<String, Method> setters = new HashMap<>();
                for (PropertyDescriptor property : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
                    if (property.getWriteMethod() != null) setters.put(key(property.getName()), property.getWriteMethod());
                }
                return Collections.unmodifiableMap(setters);
            } catch (IntrospectionException e) {
                throw new IllegalArgumentException("Cannot inspect model " + type.getName(), e);
            }
        }
    };

    public static <T> List<T> rows(Class<T> type, Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bind(statement, args);
            try (ResultSet result = statement.executeQuery()) {
                ResultSetMetaData metadata = result.getMetaData();
                Method[] setters = new Method[metadata.getColumnCount()];
                for (int i = 0; i < setters.length; i++) {
                    String column = metadata.getColumnLabel(i + 1);
                    setters[i] = SETTERS.get(type).get(key(column));
                    if (setters[i] == null) throw new SQLException("No property for column " + column + " on " + type.getName());
                }
                List<T> rows = new ArrayList<>();
                while (result.next()) {
                    try {
                        T bean = type.getDeclaredConstructor().newInstance();
                        for (int i = 0; i < setters.length; i++) {
                            setters[i].invoke(bean, convert(result.getObject(i + 1), setters[i].getParameterTypes()[0]));
                        }
                        rows.add(bean);
                    } catch (ReflectiveOperationException | IllegalArgumentException | ArithmeticException e) {
                        throw new SQLException("Cannot map query result to " + type.getName(), e);
                    }
                }
                return rows;
            }
        }
    }

    private static Object convert(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) return value;
        if (value instanceof Number) {
            BigDecimal number = new BigDecimal(value.toString());
            if (target == Long.class) return number.longValueExact();
            if (target == Integer.class) return number.intValueExact();
            if (target == Short.class) return number.shortValueExact();
            if (target == BigDecimal.class) return number;
        }
        throw new IllegalArgumentException("Cannot convert " + value.getClass().getName() + " to " + target.getName());
    }

    public static <T> T one(Class<T> type, Connection c, String sql, Object... args) throws SQLException {
        List<T> result = rows(type, c, sql, args);
        return result.isEmpty() ? null : result.get(0);
    }

    public static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql)) {
            bind(statement, args);
            return statement.executeUpdate();
        }
    }

    public static long insert(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement statement = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, args);
            statement.executeUpdate();
            try (ResultSet result = statement.getGeneratedKeys()) {
                if (!result.next()) throw new SQLException("Insert did not return a generated key");
                return result.getLong(1);
            }
        }
    }

    private static void bind(PreparedStatement statement, Object[] args) throws SQLException {
        for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
    }
}

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {

    Order findOrderByReference(String reference) {
        DBConnection dbConnection = new DBConnection();
        try (Connection connection = dbConnection.getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    select id, reference, creation_datetime, type, status
                    from "order" where reference like ?""");
            preparedStatement.setString(1, reference);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Order order = new Order();
                Integer idOrder = resultSet.getInt("id");
                order.setId(idOrder);
                order.setReference(resultSet.getString("reference"));
                order.setCreationDatetime(resultSet.getTimestamp("creation_datetime").toInstant());
                order.setType(OrderTypeEnum.valueOf(resultSet.getString("type")));
                order.setStatus(OrderStatusEnum.valueOf(resultSet.getString("status")));
                order.setDishOrderList(findDishOrderByIdOrder(idOrder));
                return order;
            }
            throw new RuntimeException("Order not found with reference " + reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    Order saveOrder(Order orderToSave) {
        try (Connection conn = new DBConnection().getConnection()) {

            if (orderToSave.getId() != null) {
                Order existing = findOrderByReference(orderToSave.getReference());
                if (existing.getStatus() == OrderStatusEnum.DELIVERED) {
                    throw new RuntimeException("A delivered order cannot be modified");
                }
            }

            String sql = """
                INSERT INTO "order" (id, reference, creation_datetime, type, status)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (id) DO UPDATE
                SET reference = EXCLUDED.reference,
                    type = EXCLUDED.type,
                    status = EXCLUDED.status
                RETURNING id
                """;

            PreparedStatement ps = conn.prepareStatement(sql);

            if (orderToSave.getId() != null) {
                ps.setInt(1, orderToSave.getId());
            } else {
                ps.setInt(1, getNextSerialValue(conn, "order", "id"));
            }

            ps.setString(2, orderToSave.getReference());
            ps.setTimestamp(3, Timestamp.from(orderToSave.getCreationDatetime()));
            ps.setString(4, orderToSave.getType().name());
            ps.setString(5, orderToSave.getStatus().name());

            ps.executeQuery();

            return findOrderByReference(orderToSave.getReference());

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DishOrder> findDishOrderByIdOrder(Integer idOrder) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishOrder> dishOrders = new ArrayList<>();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    select id, id_dish, quantity from dish_order where dish_order.id_order = ?
                    """);
            preparedStatement.setInt(1, idOrder);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                Dish dish = findDishById(resultSet.getInt("id_dish"));
                DishOrder dishOrder = new DishOrder();
                dishOrder.setId(resultSet.getInt("id"));
                dishOrder.setQuantity(resultSet.getInt("quantity"));
                dishOrder.setDish(dish);
                dishOrders.add(dishOrder);
            }
            dbConnection.closeConnection(connection);
            return dishOrders;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    Dish findDishById(Integer idDish) {
        try (Connection connection = new DBConnection().getConnection()) {
            PreparedStatement ps = connection.prepareStatement("""
                    select id, name, unit_price from dish where id = ?
                    """);
            ps.setInt(1, idDish);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Dish dish = new Dish();
                dish.setId(rs.getInt("id"));
                dish.setName(rs.getString("name"));
                dish.setUnitPrice(rs.getDouble("unit_price"));
                dish.setIngredientList(findIngredientsByDishId(idDish));
                return dish;
            }
            throw new RuntimeException("Dish not found");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Ingredient> findIngredientsByDishId(Integer idDish) {
        List<Ingredient> ingredients = new ArrayList<>();
        try (Connection connection = new DBConnection().getConnection()) {
            PreparedStatement ps = connection.prepareStatement("""
                    select i.id, i.name, i.unit_price
                    from ingredient i
                    join dish_ingredient di on di.id_ingredient = i.id
                    where di.id_dish = ?
                    """);
            ps.setInt(1, idDish);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ingredient ingredient = new Ingredient();
                ingredient.setId(rs.getInt("id"));
                ingredient.setName(rs.getString("name"));
                ingredient.setUnitPrice(rs.getDouble("unit_price"));
                ingredients.add(ingredient);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ingredients;
    }

    Integer getNextSerialValue(Connection connection, String table, String column) throws SQLException {
        PreparedStatement ps = connection.prepareStatement(
                "select nextval(pg_get_serial_sequence(?, ?))");
        ps.setString(1, table);
        ps.setString(2, column);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1);
        }
        throw new RuntimeException("Cannot get next serial value");
    }
}

package org.vykhryst.dao.mysqlEntityDao;


import org.vykhryst.dao.entityDao.AdvertisingDAO;
import org.vykhryst.entity.Advertising;
import org.vykhryst.entity.Category;
import org.vykhryst.observer.EventNotifier;
import org.vykhryst.util.DBException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySqlAdvertisingDAO extends EventNotifier<Advertising> implements AdvertisingDAO  {
    private static final String SELECT_ALL_AD = "SELECT a.id, c.id, c.name, a.name, a.measurement, a.unit_price, a.description, a.updated_at FROM advertising a LEFT JOIN category c ON a.category_id = c.id";
    private static final String SELECT_AD_BY_ID = "SELECT a.id, c.id, c.name, a.name, a.measurement, a.unit_price, a.description, a.updated_at  FROM advertising a LEFT JOIN category c ON a.category_id = c.id WHERE a.id = ?;";
    private static final String SELECT_AD_BY_NAME = "SELECT a.id, c.id, c.name, a.name, a.measurement, a.unit_price, a.description, a.updated_at  FROM advertising a LEFT JOIN category c ON a.category_id = c.id WHERE a.name = ?;";
    private static final String INSERT_AD = "INSERT INTO advertising (category_id, name, measurement, unit_price, description) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE_AD = "UPDATE advertising SET category_id = ?, name = ?, measurement = ?, unit_price = ?, description = ? WHERE id = ?";
    private static final String DELETE_AD_BY_ID = "DELETE FROM advertising WHERE id = ?";
    private final ConnectionManager connectionManager;

    public MySqlAdvertisingDAO() {
        this.connectionManager = ConnectionManager.getInstance();
    }


    @Override
    public List<Advertising> findAll() {
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_AD)) {
            List<Advertising> result = new ArrayList<>();
            while (rs.next()) {
                Advertising advertising = mapAdvertising(rs);
                result.add(advertising);
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DBException("Can't get all advertising", e);
        }
    }

    private static Advertising mapAdvertising(ResultSet rs) throws SQLException {
        return new Advertising.Builder(
                new Category(rs.getInt("c.id"), rs.getString("c.name")),
                rs.getString("a.name"),
                rs.getString("a.measurement"),
                rs.getBigDecimal("a.unit_price"),
                rs.getString("a.description"))
                .id(rs.getInt("a.id"))
                .updatedAt(rs.getTimestamp("a.updated_at").toLocalDateTime())
                .build();
    }


    @Override
    public Optional<Advertising> findById(long id) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_AD_BY_ID)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapAdvertising(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Can't get advertising by id", e);
        }
    }

    @Override
    public long save(Advertising advertising) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT_AD, Statement.RETURN_GENERATED_KEYS)) {
            setStatement(advertising, stmt);
            stmt.executeUpdate();
            ResultSet programKeys = stmt.getGeneratedKeys();
            if (programKeys.next()) {
                advertising.setId(programKeys.getInt(1));
            }
            notifyEntityAdded(advertising);
            return advertising.getId();
        } catch (SQLException e) {
            throw new DBException("Can't insert advertising", e);
        }
    }

    private static void setStatement(Advertising advertising, PreparedStatement stmt) throws SQLException {
        stmt.setLong(1, advertising.getCategory().getId());
        stmt.setString(2, advertising.getName());
        stmt.setString(3, advertising.getMeasurement());
        stmt.setBigDecimal(4, advertising.getUnitPrice());
        stmt.setString(5, advertising.getDescription());
    }

    @Override
    public boolean update(Advertising advertising) throws DBException {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(UPDATE_AD)) {
            setStatement(advertising, stmt);
            stmt.setLong(6, advertising.getId());
            notifyEntityUpdated(advertising);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DBException("Can't update advertising", e);
        }
    }

    @Override
    public boolean delete(long id) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(DELETE_AD_BY_ID)) {
            stmt.setLong(1, id);
            notifyEntityDeleted(id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new DBException("Can't delete advertising", e);
        }
    }


    @Override
    public Optional<Advertising> findByName(String name) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_AD_BY_NAME)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapAdvertising(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DBException("Can't get advertising by name", e);
        }

    }
}

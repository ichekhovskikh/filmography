package dao;

import entitiy.Franchise;
import util.dbdrivers.DbManager;
import entitiy.filter.FranchiseFilter;
import entitiy.filter.EntityFilter;
import util.EntityUtils;
import util.dbdrivers.PostgresManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class FranchiseDao implements EntityDao<Franchise> {
    private Connection connection;
    private Statement statement;
    private List<Franchise> franchises;

    public FranchiseDao(Properties properties) throws SQLException {
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");
        String host = properties.getProperty("host");
        int port = Integer.parseInt(properties.getProperty("port"));
        String sid = properties.getProperty("sid");

        DbManager manager = new PostgresManager(username, password);
        connection = manager.connect(host, port, sid);
        statement = connection.createStatement();
        franchises = new ArrayList<>();
    }

    public void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed())
            connection.close();
    }

    public List<Franchise> getAll() throws SQLException, ParseException {
        franchises.clear();
        ResultSet set;
        set = statement.executeQuery("SELECT * FROM franchise " +
                "FULL JOIN country ON country.country_id = franchise.country_id");
        while (set.next())
            franchises.add(new Franchise(set.getInt("franchise_id"),
                    set.getString("franchise_name"),
                    set.getString("country_name"),
                    EntityUtils.parseFullDate(set.getString("release"))));
        return franchises;
    }

    public Franchise get(int id) throws SQLException, ParseException {
        ResultSet set;
        set = statement.executeQuery("SELECT * FROM franchise " +
                "FULL JOIN country ON country.country_id = franchise.country_id " +
                "WHERE franchise_id=" + id);
        set.next();
        return new Franchise(set.getInt("franchise_id"),
                set.getString("franchise_name"),
                set.getString("country_name"),
                EntityUtils.parseFullDate(set.getString("release")));
    }

    public void add(Franchise franchise) throws SQLException {
        try {
            statement.executeUpdate("INSERT INTO country(country_name) values('" + franchise.getCountry() + "')");
        } finally {
            statement.executeUpdate("INSERT INTO franchise(franchise_name, country, release) values('" + franchise.getFranchiseName() +
                    ", SELECT TOP 1 country_id FROM country WHERE country_id=" + franchise.getCountry() +
                    "',to_date('" + franchise.dateToString() + "', 'yyyy-MM-dd'))");
        }
    }

    public void addAll(List<Franchise> franchises) throws SQLException {
        String sqlRequest = "INSERT INTO franchise(franchise_name, country, release) values ";
        String sqlCountryRequest = "INSERT INTO country(country_name) values ";
        for (int i = 0, size = franchises.size(); i < size; i++) {
            Franchise franchise = franchises.get(i);

            sqlRequest += "('" + franchise.getFranchiseName() +
                    "', SELECT TOP 1 country_id FROM country WHERE country_id=" + franchise.getCountry() +
                    "',to_date('" + franchise.dateToString() + "', 'yyyy-MM-dd'))";

            sqlCountryRequest += "('" + franchise.getCountry() + "')";
            if (i != size - 1) {
                sqlRequest += ",";
                sqlCountryRequest += ",";
            }
        }
        try {
            statement.executeUpdate(sqlCountryRequest);
        } finally {
            statement.executeUpdate(sqlRequest);
        }
    }

    public void remove(int id) throws SQLException {
        statement.executeUpdate("DELETE FROM franchise WHERE franchise_id=" + id);
    }

    public void update(Franchise franchise) throws SQLException {
        statement.executeUpdate("UPDATE franchise " +
                "FULL JOIN country ON country.country_id = franchise.country_id " +
                "SET franchise_name='" + franchise.getFranchiseName() +
                "', release=to_date('" + franchise.dateToString() + "', 'yyyy-MM-dd')" +
                "', country_name='" + franchise.getCountry() +
                "' WHERE franchise_id=" + franchise.getId());
    }

    public List<Franchise> getByName(String name) throws SQLException, ParseException {
        franchises.clear();
        ResultSet set;
        set = statement.executeQuery("SELECT * FROM franchise " +
                "FULL JOIN country ON country.country_id = franchise.country_id WHERE " +
                regexpLike("franchise.franchise_name", name));
        while (set.next())
            franchises.add(new Franchise(set.getInt("franchise_id"),
                    set.getString("franchise_name"),
                    set.getString("country_name"),
                    EntityUtils.parseFullDate(set.getString("release"))));
        return franchises;
    }

    public List<Franchise> getByFilter(EntityFilter entityFilter) throws SQLException, ParseException {
        FranchiseFilter filter = (FranchiseFilter) entityFilter;
        franchises.clear();
        ResultSet set;
        set = statement.executeQuery("SELECT * FROM franchise WHERE " +
                "FULL JOIN country ON country.country_id = franchise.country_id WHERE " +
                (EntityUtils.formatTime(filter.getRelease()).equals("59:59") ? "" : "TO_CHAR(franchise.release,'yyyy-MM-dd') = '" + filter.dateToString() + "' AND ") +
                regexpLike("franchise.franchise_name", filter.getName()) + " AND " +
                regexpLike("country.country_name", filter.getCountry()));
        while (set.next())
            franchises.add(new Franchise(set.getInt("franchise_id"),
                    set.getString("franchise_name"),
                    set.getString("country_name"),
                    EntityUtils.parseFullDate(set.getString("release"))));
        return franchises;
    }

    private String regexpLike(String attribute, String value) {
        return "UPPER(" + attribute + ") LIKE UPPER('%" + value + "%')";
    }
}
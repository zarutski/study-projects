package make.project.city.dao;

import make.project.city.exception.PersonCheckException;
import make.project.city.domain.PersonRequest;
import make.project.city.domain.PersonResponse;

import java.sql.*;

public class PersonCheckDao {

    private static final String SQL_REQUEST =
            "SELECT temporal FROM cr_address_person AS ap" +
                    " INNER JOIN cr_person AS p ON p.person_id = ap.person_id" +
                    " INNER JOIN cr_address AS a ON a.address_id = ap.address_id" +
                    " WHERE" +
                    " CURRENT_DATE >= ap.start_date" +
                    " and (CURRENT_DATE <= ap.end_date or ap.end_date is null)" +
                    " and upper(p.sur_name) = upper(?)" +
                    " and upper(p.given_name) = upper(?)" +
                    " and upper(p.patronymic) = upper(?)" +
                    " and p.date_of_birth = ?" +
                    " and a.street_code = ?" +
                    " and upper(a.building) = upper(?)";

    private ConnectionBuilder connectionBuilder;

    public void setConnectionBuilder(ConnectionBuilder connectionBuilder) {
        this.connectionBuilder = connectionBuilder;
    }

    private Connection getConnection() throws SQLException {
        return connectionBuilder.getConnection();
    }

    public PersonResponse checkPerson(PersonRequest request) throws PersonCheckException {
        PersonResponse response = new PersonResponse();
        String sql = SQL_REQUEST;
        boolean hasExtension = request.getExtension() != null;
        boolean hasApartment = request.getApartment() != null;
        if (hasExtension) {
            sql += " and upper(a.extension) = upper(?)";
        } else {
            sql += " and a.extension IS NULL";
        }
        if (hasApartment) {
            sql += " and upper(a.apartment) = upper(?)";
        } else {
            sql += " and a.apartment IS NULL";
        }

        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {

            int count = 1;
            stmt.setString(count++, request.getSurName());
            stmt.setString(count++, request.getGivenName());
            stmt.setString(count++, request.getPatronymic());
            stmt.setDate(count++, java.sql.Date.valueOf(request.getDateOfBirth()));
            stmt.setInt(count++, request.getStreetCode());
            stmt.setString(count++, request.getBuilding());
            if (hasExtension) {
                stmt.setString(count++, request.getExtension());
            }
            if (hasApartment) {
                stmt.setString(count, request.getApartment());
            }

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                response.setRegistered(true);
                response.setTemporal(rs.getBoolean("temporal"));
            }
        } catch (SQLException e) {
            throw new PersonCheckException(e);
        }

        return response;
    }
}

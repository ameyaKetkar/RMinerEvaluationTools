package com.querydsl.sql.types;

import java.sql.*;
import java.time.Instant;

import javax.annotation.Nullable;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/**
 * JSR310InstantType maps {@linkplain java.time.Instant} to
 * {@linkplain java.sql.Timestamp} on the JDBC level
 *
 */
@IgnoreJRERequirement //conditionally included
public class JSR310InstantType extends AbstractJSR310DateTimeType<Instant>  {

    public JSR310InstantType() {
        super(Types.TIMESTAMP);
    }

    public JSR310InstantType(int type) {
        super(type);
    }

    @Override
    public String getLiteral(Instant value) {
        return dateTimeFormatter.format(value);
    }

    @Override
    public Class<Instant> getReturnedClass() {
        return Instant.class;
    }

    @Nullable
    @Override
    public Instant getValue(ResultSet rs, int startIndex) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(startIndex, utc());
        return timestamp != null ? timestamp.toInstant() : null;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, Instant value) throws SQLException {
        st.setTimestamp(startIndex, Timestamp.from(value), utc());
    }
}

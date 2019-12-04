package com.querydsl.sql.types;

import java.sql.*;
import java.time.LocalTime;

import javax.annotation.Nullable;

import org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement;

/**
 * JSR310LocalTimeType maps {@linkplain java.time.LocalTime}
 * to {@linkplain java.sql.Time} on the JDBC level
 *
 */
@IgnoreJRERequirement //conditionally included
public class JSR310LocalTimeType extends AbstractJSR310DateTimeType<LocalTime> {


    public JSR310LocalTimeType() {
        super(Types.TIME);
    }

    public JSR310LocalTimeType(int type) {
        super(type);
    }

    @Override
    public String getLiteral(LocalTime value) {
        return timeFormatter.format(value);
    }

    @Override
    public Class<LocalTime> getReturnedClass() {
        return LocalTime.class;
    }

    @Nullable
    @Override
    public LocalTime getValue(ResultSet rs, int startIndex) throws SQLException {
        Time time = rs.getTime(startIndex, utc());
        return time != null ? time.toLocalTime() : null;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, LocalTime value) throws SQLException {
        st.setTime(startIndex, Time.valueOf(value), utc());
    }
}

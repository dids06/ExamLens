package com.examlens.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DbTest {

    @Test
    void mapsJdbcUrlsToDriverClasses() {
        assertEquals("com.mysql.cj.jdbc.Driver",
                Db.driverFor("jdbc:mysql://127.0.0.1:3307/examlens?useSSL=false"));
        assertEquals("org.mariadb.jdbc.Driver", Db.driverFor("jdbc:mariadb://localhost/db"));
        assertEquals("org.h2.Driver", Db.driverFor("jdbc:h2:mem:test;MODE=MySQL"));
        assertNull(Db.driverFor("jdbc:unknown:foo"));
        assertNull(Db.driverFor(null));
    }
}

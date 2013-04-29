package com.alibaba.druid.bvt.pool.exception;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.junit.Assert;

import com.alibaba.druid.mock.MockConnection;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.pool.vendor.OracleExceptionSorter;
import com.alibaba.druid.test.util.OracleMockDriver;
import com.alibaba.druid.util.JdbcUtils;

public class OracleExceptionSorterTest_closeStmt extends TestCase {

    private DruidDataSource dataSource;

    protected void setUp() throws Exception {
        dataSource = new DruidDataSource();

        dataSource.setExceptionSorter(new OracleExceptionSorter());

        dataSource.setDriver(new OracleMockDriver());
        dataSource.setUrl("jdbc:mock:xxx");
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxOpenPreparedStatements(100);
    }

    protected void tearDowN() throws Exception {
        JdbcUtils.close(dataSource);
    }

    public void test_connect() throws Exception {
        String sql = "SELECT 1";
        {
            DruidPooledConnection conn = dataSource.getConnection();

            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.execute();
            pstmt.close();
            conn.close();
        }

        DruidPooledConnection conn = dataSource.getConnection();
        MockConnection mockConn = conn.unwrap(MockConnection.class);
        Assert.assertNotNull(mockConn);

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setFetchSize(1000);

        SQLException exception = new SQLException("xx", "xxx", 28);
        mockConn.setError(exception);

        SQLException closedErrror = null;
        try {
            pstmt.close();
        } catch (SQLException ex) {
            closedErrror = ex;
        }
        Assert.assertNotNull(closedErrror);
        Assert.assertSame(exception, closedErrror);
        
        SQLException commetError = null;
        try {
            conn.commit();
        } catch (SQLException ex) {
            commetError = ex;
        }

        Assert.assertNotNull(commetError);
        Assert.assertSame(exception, commetError.getCause());

        conn.close();
    }

}
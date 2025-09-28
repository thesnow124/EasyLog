package com.github.easylog.support;

import com.alibaba.fastjson.JSON;
import com.github.easylog.model.EasyLogInfo;
import com.github.easylog.api.ILogRecordService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;

/**
 * JDBC 实现：存在 DataSource 且未自定义 ILogRecordService 时自动启用
 */
public class JdbcLogRecordServiceImpl implements ILogRecordService {

    private final DataSource dataSource;

    public JdbcLogRecordServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void record(EasyLogInfo log) {
        String sql = "INSERT INTO easy_log_record (platform, operator, operate_time, biz_no, module, type, content, content_param, execute_time, success, result, error_msg, stack_trace, ip, url, http_method, class_method, param_json, detail, field_info_json, created_at) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW())";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // 1 platform
            ps.setString(1, nullSafe(log.getPlatform()));
            // 2 operator
            ps.setString(2, nullSafe(log.getOperator()));
            // 3 operate_time
            ps.setTimestamp(3, new Timestamp(Optional.ofNullable(log.getOperateTime()).orElse(System.currentTimeMillis())));
            // 4 biz_no
            ps.setString(4, nullSafe(log.getBizNo()));
            // 5 module
            ps.setString(5, nullSafe(log.getModule()));
            // 6 type
            ps.setString(6, nullSafe(log.getType()));
            // 7 content
            ps.setString(7, nullSafe(log.getContent()));
            // 8 content_param
            ps.setString(8, log.getContentParam() == null ? null : JSON.toJSONString(log.getContentParam()));
            // 9 execute_time
            if (log.getExecuteTime() == null) ps.setObject(9, null); else ps.setLong(9, log.getExecuteTime());
            // 10 success
            if (log.getSuccess() == null) ps.setObject(10, null); else ps.setBoolean(10, log.getSuccess());
            // 11 result
            ps.setString(11, nullSafe(log.getResult()));
            // 12 error_msg
            ps.setString(12, nullSafe(log.getErrorMsg()));
            // 13 stack_trace
            ps.setString(13, nullSafe(log.getStackTrace()));
            // 14 ip
            ps.setString(14, nullSafe(log.getIp()));
            // 15 url
            ps.setString(15, nullSafe(log.getUrl()));
            // 16 http_method
            ps.setString(16, nullSafe(log.getHttpMethod()));
            // 17 class_method
            ps.setString(17, nullSafe(log.getClassMethod()));
            // 18 param_json
            ps.setString(18, log.getParam() == null ? null : JSON.toJSONString(log.getParam()));
            // 19 detail
            ps.setString(19, nullSafe(log.getDetail()));
            // 20 field_info_json
            ps.setString(20, log.getFieldInfoList() == null ? null : JSON.toJSONString(log.getFieldInfoList()));

            ps.executeUpdate();
        } catch (SQLException e) {
            // 不影响业务：降级为忽略或可改为日志打印
            // 这里不抛出异常，避免影响业务事务
        }
    }

    private String nullSafe(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}

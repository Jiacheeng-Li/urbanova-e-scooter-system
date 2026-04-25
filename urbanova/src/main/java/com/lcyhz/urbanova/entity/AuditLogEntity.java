package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_logs")
public class AuditLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String auditLogId;
    private String actorUserId;
    private String actorRole;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private LocalDateTime createdAt;
}

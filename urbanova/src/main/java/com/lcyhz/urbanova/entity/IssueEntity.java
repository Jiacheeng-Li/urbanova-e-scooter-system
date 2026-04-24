package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("issues")
public class IssueEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String issueId;
    private String reporterUserId;
    private String bookingId;
    private String scooterId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private String managerFeedback;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

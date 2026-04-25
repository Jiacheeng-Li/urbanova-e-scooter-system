package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("issue_comments")
public class IssueCommentEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String commentId;
    private String issueId;
    private String authorUserId;
    private String authorRole;
    private String message;
    private LocalDateTime createdAt;
}

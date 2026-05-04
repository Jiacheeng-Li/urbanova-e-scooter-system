package com.lcyhz.urbanova.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("issue_photos")
public class IssuePhotoEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String photoId;
    private String issueId;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private String storagePath;
    private LocalDateTime createdAt;
}

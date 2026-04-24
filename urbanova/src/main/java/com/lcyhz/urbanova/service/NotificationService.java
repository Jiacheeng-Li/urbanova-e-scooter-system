package com.lcyhz.urbanova.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.common.exception.ErrorCodes;
import com.lcyhz.urbanova.entity.NotificationEntity;
import com.lcyhz.urbanova.mapper.NotificationMapper;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationMapper notificationMapper;
    private final PlatformSupportService platformSupportService;

    public NotificationService(NotificationMapper notificationMapper, PlatformSupportService platformSupportService) {
        this.notificationMapper = notificationMapper;
        this.platformSupportService = platformSupportService;
    }

    public List<Map<String, Object>> listNotifications(String userId) {
        return platformSupportService.listNotifications(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markRead(String userId, String notificationId) {
        NotificationEntity notification = notificationMapper.selectOne(new LambdaQueryWrapper<NotificationEntity>()
                .eq(NotificationEntity::getNotificationId, notificationId)
                .eq(NotificationEntity::getUserId, userId));
        if (notification == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND.value(), ErrorCodes.RESOURCE_NOT_FOUND, "Notification not found");
        }
        notification.setReadFlag(1);
        notification.setUpdatedAt(LocalDateTime.now());
        notificationMapper.updateById(notification);
        return platformSupportService.toNotificationMap(notification);
    }
}

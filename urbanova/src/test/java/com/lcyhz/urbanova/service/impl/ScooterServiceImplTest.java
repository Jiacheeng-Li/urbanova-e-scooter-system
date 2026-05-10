package com.lcyhz.urbanova.service.impl;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.admin.scooter.BulkUpdateScooterStatusRequest;
import com.lcyhz.urbanova.dto.admin.scooter.CreateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterRequest;
import com.lcyhz.urbanova.dto.admin.scooter.UpdateScooterStatusRequest;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.entity.ScooterTypeEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.mapper.ScooterTypeMapper;
import com.lcyhz.urbanova.mapper.UserLocationMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import com.lcyhz.urbanova.vo.scooter.AdminScooterVo;
import com.lcyhz.urbanova.vo.scooter.BulkScooterStatusUpdateVo;
import com.lcyhz.urbanova.vo.scooter.ScooterIdsByStatusVo;
import com.lcyhz.urbanova.vo.scooter.ScooterMapPointVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScooterServiceImpl Tests")
class ScooterServiceImplTest {

    @Mock private ScooterMapper scooterMapper;
    @Mock private ScooterTypeMapper scooterTypeMapper;
    @Mock private UserMapper userMapper;
    @Mock private UserLocationMapper userLocationMapper;
    @Mock private PlatformSupportService platformSupportService;

    @InjectMocks
    private ScooterServiceImpl scooterService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scooterService, "lowBatteryThreshold", 20);
        ReflectionTestUtils.setField(scooterService, "batteryDrainPerMinute", 1);
        ReflectionTestUtils.setField(scooterService, "chargeDurationMinutes", 3);
    }

    // ─── Helper builders ─────────────────────────────────────────────────

    private ScooterEntity buildScooter(String scooterId, String status, int battery) {
        ScooterEntity s = new ScooterEntity();
        s.setScooterId(scooterId);
        s.setTypeCode("ANDROMEDA");
        s.setStatus(status);
        s.setBatteryPercent(battery);
        s.setLat(new BigDecimal("51.5074"));
        s.setLng(new BigDecimal("-0.1278"));
        s.setZone("ZONE-A");
        s.setVersion(0);
        s.setQrCodeId("QR-TESTQRCODE01");
        s.setBatteryUpdatedAt(LocalDateTime.now().minusMinutes(5));
        s.setCreatedAt(LocalDateTime.now().minusDays(10));
        s.setUpdatedAt(LocalDateTime.now());
        return s;
    }

    private ScooterTypeEntity buildScooterType(String code) {
        ScooterTypeEntity t = new ScooterTypeEntity();
        t.setTypeCode(code);
        t.setDisplayName("Andromeda Model");
        t.setImageUrl("https://example.com/andromeda.png");
        t.setDescription("A sleek scooter");
        t.setActive(1);
        return t;
    }

    // ─── createScooter ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createScooter")
    class CreateScooter {

        @Test
        @DisplayName("正常创建 → scooterId 转大写，默认电量 100%，状态 AVAILABLE")
        void success_fullBattery_statusAvailable() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("sc-new-001"); // 小写，应被转大写
            req.setTypeCode("ANDROMEDA");
            req.setLat(new BigDecimal("51.5074"));
            req.setLng(new BigDecimal("-0.1278"));

            when(scooterMapper.selectOne(any())).thenReturn(null); // 无重复
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.insert(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.createScooter(req);

            assertThat(result.getScooterId()).isEqualTo("SC-NEW-001");
            assertThat(result.getBatteryPercent()).isEqualTo(100);
            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.AVAILABLE);
        }

        @Test
        @DisplayName("创建时电量低于阈值（10% < 20%）→ 状态自动设为 LOW_BATTERY")
        void lowBattery_statusSetToLowBattery() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("SC-LOW-001");
            req.setTypeCode("ANDROMEDA");
            req.setBatteryPercent(10); // 低于 20% 阈值
            req.setLat(new BigDecimal("51.5074"));
            req.setLng(new BigDecimal("-0.1278"));

            when(scooterMapper.selectOne(any())).thenReturn(null);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.insert(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.createScooter(req);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.LOW_BATTERY);
        }

        @Test
        @DisplayName("创建时指定状态 MAINTENANCE → 使用指定状态")
        void specifiedStatus_usesSpecifiedStatus() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("SC-MAINT-001");
            req.setTypeCode("ANDROMEDA");
            req.setStatus("MAINTENANCE");
            req.setLat(new BigDecimal("51.5074"));
            req.setLng(new BigDecimal("-0.1278"));

            when(scooterMapper.selectOne(any())).thenReturn(null);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.insert(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.createScooter(req);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("scooterId 重复 → 抛 BusinessException")
        void duplicateScooterId_throws() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("SC-001");
            req.setTypeCode("ANDROMEDA");

            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80));

            assertThatThrownBy(() -> scooterService.createScooter(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("typeCode 不存在 → 抛 BusinessException")
        void invalidTypeCode_throws() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("SC-002");
            req.setTypeCode("NONEXISTENT");
            req.setLat(new BigDecimal("51.5074"));
            req.setLng(new BigDecimal("-0.1278"));

            when(scooterMapper.selectOne(any())).thenReturn(null);
            when(scooterTypeMapper.selectOne(any())).thenReturn(null); // type 不存在

            assertThatThrownBy(() -> scooterService.createScooter(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("只传 lat 不传 lng → 抛 BusinessException（坐标必须成对）")
        void onlyLat_throws() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("SC-003");
            req.setTypeCode("ANDROMEDA");
            req.setLat(new BigDecimal("51.5074")); // 只有 lat，没有 lng

            assertThatThrownBy(() -> scooterService.createScooter(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("lat 超出合法范围（>90）→ 抛 BusinessException")
        void latOutOfRange_throws() {
            CreateScooterRequest req = new CreateScooterRequest();
            req.setScooterId("SC-004");
            req.setTypeCode("ANDROMEDA");
            req.setLat(new BigDecimal("91.0")); // 超出 [-90, 90]
            req.setLng(new BigDecimal("0.0"));

            assertThatThrownBy(() -> scooterService.createScooter(req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── updateScooter ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateScooter")
    class UpdateScooter {

        @Test
        @DisplayName("request 为 null → 抛 BusinessException")
        void nullRequest_throws() {
            assertThatThrownBy(() -> scooterService.updateScooter("SC-001", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("request 无任何字段 → 抛 BusinessException")
        void emptyRequest_throws() {
            UpdateScooterRequest req = new UpdateScooterRequest(); // 所有字段为 null

            assertThatThrownBy(() -> scooterService.updateScooter("SC-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("scooter 不存在 → 抛 BusinessException")
        void scooterNotFound_throws() {
            UpdateScooterRequest req = new UpdateScooterRequest();
            req.setBatteryPercent(80);

            when(scooterMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> scooterService.updateScooter("SC-NONE", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("更新电量至高于阈值 → 状态从 LOW_BATTERY 变为 AVAILABLE")
        void batteryAboveThreshold_statusBecomesAvailable() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.LOW_BATTERY, 10);
            UpdateScooterRequest req = new UpdateScooterRequest();
            req.setBatteryPercent(80); // 高于 20% 阈值

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.updateScooter("SC-001", req);

            assertThat(result.getBatteryPercent()).isEqualTo(80);
            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.AVAILABLE);
        }

        @Test
        @DisplayName("更新电量至低于阈值 → 状态从 AVAILABLE 变为 LOW_BATTERY")
        void batteryBelowThreshold_statusBecomesLowBattery() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80);
            UpdateScooterRequest req = new UpdateScooterRequest();
            req.setBatteryPercent(5); // 低于 20% 阈值

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.updateScooter("SC-001", req);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.LOW_BATTERY);
        }

        @Test
        @DisplayName("更新区域信息 → zone 值正确保存")
        void updateZone_succeeds() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80);
            UpdateScooterRequest req = new UpdateScooterRequest();
            req.setZone("ZONE-B");

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.updateScooter("SC-001", req);

            assertThat(result.getZone()).isEqualTo("ZONE-B");
        }
    }

    // ─── updateScooterStatus ──────────────────────────────────────────────

    @Nested
    @DisplayName("updateScooterStatus")
    class UpdateScooterStatus {

        @Test
        @DisplayName("状态改为 MAINTENANCE → 成功更新")
        void toMaintenance_succeeds() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80);
            UpdateScooterStatusRequest req = new UpdateScooterStatusRequest();
            req.setStatus("MAINTENANCE");

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.updateScooterStatus("SC-001", req);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("状态改为 CHARGING → chargeStartedAt 被设置")
        void toCharging_setsChargeStartedAt() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.LOW_BATTERY, 10);
            UpdateScooterStatusRequest req = new UpdateScooterStatusRequest();
            req.setStatus("CHARGING");

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            AdminScooterVo result = scooterService.updateScooterStatus("SC-001", req);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.CHARGING);
        }

        @Test
        @DisplayName("非法状态 → 抛 BusinessException")
        void invalidStatus_throws() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80);
            UpdateScooterStatusRequest req = new UpdateScooterStatusRequest();
            req.setStatus("FLYING"); // 非法

            when(scooterMapper.selectOne(any())).thenReturn(entity);

            assertThatThrownBy(() -> scooterService.updateScooterStatus("SC-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("scooter 不存在 → 抛 BusinessException")
        void notFound_throws() {
            UpdateScooterStatusRequest req = new UpdateScooterStatusRequest();
            req.setStatus("AVAILABLE");

            when(scooterMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> scooterService.updateScooterStatus("SC-NONE", req))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── bulkUpdateScooterStatus ──────────────────────────────────────────

    @Nested
    @DisplayName("bulkUpdateScooterStatus")
    class BulkUpdateScooterStatus {

        @Test
        @DisplayName("全部 scooter 存在 → 成功更新，返回正确数量")
        void allFound_returnsCorrectCount() {
            BulkUpdateScooterStatusRequest req = new BulkUpdateScooterStatusRequest();
            req.setScooterIds(List.of("SC-001", "SC-002"));
            req.setStatus("MAINTENANCE");

            when(scooterMapper.selectList(any())).thenReturn(List.of(
                    buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80),
                    buildScooter("SC-002", DomainConstants.ScooterStatus.AVAILABLE, 70)
            ));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            BulkScooterStatusUpdateVo result = scooterService.bulkUpdateScooterStatus(req);

            assertThat(result.getUpdatedCount()).isEqualTo(2);
            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.MAINTENANCE);
            assertThat(result.getScooterIds()).containsExactlyInAnyOrder("SC-001", "SC-002");
        }

        @Test
        @DisplayName("部分 scooter 不存在 → 抛 BusinessException，包含缺失的 ID")
        void someNotFound_throws() {
            BulkUpdateScooterStatusRequest req = new BulkUpdateScooterStatusRequest();
            req.setScooterIds(List.of("SC-001", "SC-MISSING"));
            req.setStatus("MAINTENANCE");

            when(scooterMapper.selectList(any())).thenReturn(List.of(
                    buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80)
            )); // 只找到 1 个，SC-MISSING 不在 DB 里

            assertThatThrownBy(() -> scooterService.bulkUpdateScooterStatus(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("非法 status → 抛 BusinessException")
        void invalidStatus_throws() {
            BulkUpdateScooterStatusRequest req = new BulkUpdateScooterStatusRequest();
            req.setScooterIds(List.of("SC-001"));
            req.setStatus("INVALID_STATUS");

            assertThatThrownBy(() -> scooterService.bulkUpdateScooterStatus(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("scooterIds 中有重复 → 去重后只更新一次")
        void duplicateIds_deduplicatedBeforeUpdate() {
            BulkUpdateScooterStatusRequest req = new BulkUpdateScooterStatusRequest();
            req.setScooterIds(List.of("SC-001", "SC-001")); // 重复
            req.setStatus("MAINTENANCE");

            when(scooterMapper.selectList(any())).thenReturn(List.of(
                    buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80)
            ));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            BulkScooterStatusUpdateVo result = scooterService.bulkUpdateScooterStatus(req);

            assertThat(result.getUpdatedCount()).isEqualTo(1);
        }
    }

    // ─── queryScooterIdsByStatus ──────────────────────────────────────────

    @Nested
    @DisplayName("queryScooterIdsByStatus")
    class QueryScooterIdsByStatus {

        @Test
        @DisplayName("合法状态 → 返回对应 scooter ID 列表")
        void validStatus_returnsIds() {
            when(scooterMapper.selectList(any())).thenReturn(List.of(
                    buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80),
                    buildScooter("SC-002", DomainConstants.ScooterStatus.AVAILABLE, 90)
            ));

            ScooterIdsByStatusVo result = scooterService.queryScooterIdsByStatus("AVAILABLE");

            assertThat(result.getStatus()).isEqualTo(DomainConstants.ScooterStatus.AVAILABLE);
            assertThat(result.getScooterIds()).containsExactlyInAnyOrder("SC-001", "SC-002");
        }

        @Test
        @DisplayName("非法状态 → 抛 BusinessException")
        void invalidStatus_throws() {
            assertThatThrownBy(() -> scooterService.queryScooterIdsByStatus("NONSENSE"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("该状态无 scooter → 返回空列表")
        void noScootersWithStatus_returnsEmpty() {
            when(scooterMapper.selectList(any())).thenReturn(List.of());

            ScooterIdsByStatusVo result = scooterService.queryScooterIdsByStatus("IN_USE");

            assertThat(result.getScooterIds()).isEmpty();
        }
    }

    // ─── startCharging ────────────────────────────────────────────────────

    @Nested
    @DisplayName("startCharging")
    class StartCharging {

        @Test
        @DisplayName("LOW_BATTERY 状态可以开始充电 → 状态变为 CHARGING")
        void lowBattery_canCharge() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.LOW_BATTERY, 10);

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            Map<String, Object> result = scooterService.startCharging("SC-001");

            assertThat(result.get("status")).isEqualTo(DomainConstants.ScooterStatus.CHARGING);
        }

        @Test
        @DisplayName("AVAILABLE 状态可以开始充电")
        void available_canCharge() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 50);

            when(scooterMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildScooterType("ANDROMEDA"));
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            Map<String, Object> result = scooterService.startCharging("SC-001");

            assertThat(result.get("status")).isEqualTo(DomainConstants.ScooterStatus.CHARGING);
        }

        @Test
        @DisplayName("RESERVED 状态不能充电 → 抛 BusinessException")
        void reserved_cannotCharge() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.RESERVED, 80);

            when(scooterMapper.selectOne(any())).thenReturn(entity);

            assertThatThrownBy(() -> scooterService.startCharging("SC-001"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("IN_USE 状态不能充电 → 抛 BusinessException")
        void inUse_cannotCharge() {
            ScooterEntity entity = buildScooter("SC-001", DomainConstants.ScooterStatus.IN_USE, 50);

            when(scooterMapper.selectOne(any())).thenReturn(entity);

            assertThatThrownBy(() -> scooterService.startCharging("SC-001"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("scooter 不存在 → 抛 BusinessException")
        void notFound_throws() {
            when(scooterMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> scooterService.startCharging("SC-NONE"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── listMapPoints ────────────────────────────────────────────────────

    @Nested
    @DisplayName("listMapPoints")
    class ListMapPoints {

        @Test
        @DisplayName("有坐标的 scooter → 返回地图点列表")
        void scootersWithCoords_returnsMapPoints() {
            when(scooterMapper.selectList(any())).thenReturn(List.of(
                    buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80),
                    buildScooter("SC-002", DomainConstants.ScooterStatus.LOW_BATTERY, 15)
            ));
            when(scooterTypeMapper.selectList(any())).thenReturn(List.of(buildScooterType("ANDROMEDA")));

            List<ScooterMapPointVo> result = scooterService.listMapPoints();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getScooterId()).isEqualTo("SC-001");
        }

        @Test
        @DisplayName("无 scooter → 返回空列表")
        void noScooters_returnsEmpty() {
            when(scooterMapper.selectList(any())).thenReturn(List.of());
            when(scooterTypeMapper.selectList(any())).thenReturn(List.of());

            assertThat(scooterService.listMapPoints()).isEmpty();
        }
    }

    // ─── processScooterLifecycle ──────────────────────────────────────────

    @Nested
    @DisplayName("processScooterLifecycle")
    class ProcessScooterLifecycle {

        @Test
        @DisplayName("IN_USE 的 scooter 经过时间后电量下降并写库")
        void inUse_batteryDrains() {
            ScooterEntity inUse = buildScooter("SC-001", DomainConstants.ScooterStatus.IN_USE, 80);
            inUse.setBatteryUpdatedAt(LocalDateTime.now().minusMinutes(10)); // 10分钟前更新过

            when(scooterMapper.selectList(any())).thenReturn(List.of(inUse), List.of()); // 第一次 IN_USE，第二次 CHARGING 为空

            scooterService.processScooterLifecycle();

            verify(scooterMapper, atLeastOnce()).updateById(any(ScooterEntity.class));
        }

        @Test
        @DisplayName("CHARGING 且充电时间达到 chargeDurationMinutes → 电量恢复 100%，状态变 AVAILABLE")
        void charging_completesAfterDuration() {
            ScooterEntity charging = buildScooter("SC-001", DomainConstants.ScooterStatus.CHARGING, 15);
            charging.setChargeStartedAt(LocalDateTime.now().minusMinutes(10)); // 10分钟前开始，> chargeDurationMinutes(3)

            when(scooterMapper.selectList(any()))
                    .thenReturn(List.of()) // IN_USE 列表为空
                    .thenReturn(List.of(charging)); // CHARGING 列表
            when(userMapper.selectList(any())).thenReturn(List.of()); // 无 manager
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);

            scooterService.processScooterLifecycle();

            verify(scooterMapper, atLeastOnce()).updateById(any(ScooterEntity.class));
            assertThat(charging.getBatteryPercent()).isEqualTo(100);
            assertThat(charging.getStatus()).isEqualTo(DomainConstants.ScooterStatus.AVAILABLE);
        }

        @Test
        @DisplayName("CHARGING 且充电时间未达到 → 不更新电量")
        void charging_notYetComplete_noUpdate() {
            ScooterEntity charging = buildScooter("SC-001", DomainConstants.ScooterStatus.CHARGING, 15);
            charging.setChargeStartedAt(LocalDateTime.now().minusMinutes(1)); // 1分钟前，< chargeDurationMinutes(3)

            when(scooterMapper.selectList(any()))
                    .thenReturn(List.of()) // IN_USE 空
                    .thenReturn(List.of(charging)); // CHARGING

            scooterService.processScooterLifecycle();

            assertThat(charging.getBatteryPercent()).isEqualTo(15); // 未变
            assertThat(charging.getStatus()).isEqualTo(DomainConstants.ScooterStatus.CHARGING); // 未变
        }
    }

    // ─── findNearbyScooters ───────────────────────────────────────────────

    @Nested
    @DisplayName("findNearbyScooters")
    class FindNearbyScooters {

        @Test
        @DisplayName("半径范围内的 scooter → 被返回")
        void withinRadius_returned() {
            // 伦敦中心附近的 scooter
            ScooterEntity nearby = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80);
            nearby.setLat(new BigDecimal("51.5080")); // 非常接近中心点
            nearby.setLng(new BigDecimal("-0.1280"));

            when(scooterMapper.selectList(any())).thenReturn(List.of(nearby));

            BigDecimal centerLat = new BigDecimal("51.5074");
            BigDecimal centerLng = new BigDecimal("-0.1278");

            List<ScooterEntity> result = scooterService.findNearbyScooters(
                    centerLat.subtract(BigDecimal.ONE), centerLat.add(BigDecimal.ONE),
                    centerLng.subtract(BigDecimal.ONE), centerLng.add(BigDecimal.ONE),
                    centerLat, centerLng,
                    5.0 // 5km 半径
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScooterId()).isEqualTo("SC-001");
        }

        @Test
        @DisplayName("超出半径范围的 scooter → 不被返回")
        void outsideRadius_notReturned() {
            // 模拟 DB 返回了一个范围内（经纬度 bounding box 内）的 scooter，但实际距离超出半径
            ScooterEntity farAway = buildScooter("SC-FAR", DomainConstants.ScooterStatus.AVAILABLE, 80);
            farAway.setLat(new BigDecimal("52.5")); // 距中心约 110km
            farAway.setLng(new BigDecimal("-0.1278"));

            when(scooterMapper.selectList(any())).thenReturn(List.of(farAway));

            BigDecimal centerLat = new BigDecimal("51.5074");
            BigDecimal centerLng = new BigDecimal("-0.1278");

            List<ScooterEntity> result = scooterService.findNearbyScooters(
                    centerLat.subtract(BigDecimal.TEN), centerLat.add(BigDecimal.TEN),
                    centerLng.subtract(BigDecimal.TEN), centerLng.add(BigDecimal.TEN),
                    centerLat, centerLng,
                    1.0 // 只有 1km 半径
            );

            assertThat(result).isEmpty();
        }
    }
}

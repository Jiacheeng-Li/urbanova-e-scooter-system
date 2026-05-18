package com.lcyhz.urbanova.service.impl;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.dto.admin.scootertype.CreateScooterTypeRequest;
import com.lcyhz.urbanova.dto.admin.scootertype.UpdateScooterTypeRequest;
import com.lcyhz.urbanova.entity.ScooterTypeEntity;
import com.lcyhz.urbanova.mapper.ScooterTypeMapper;
import com.lcyhz.urbanova.vo.scooter.AdminScooterTypeVo;
import com.lcyhz.urbanova.vo.scooter.ScooterTypeVo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScooterTypeServiceImpl Tests")
class ScooterTypeServiceImplTest {

    @Mock private ScooterTypeMapper scooterTypeMapper;

    @InjectMocks
    private ScooterTypeServiceImpl scooterTypeService;

    // ─── Helper builders ─────────────────────────────────────────────────

    private ScooterTypeEntity buildType(String code, String name, int active) {
        ScooterTypeEntity e = new ScooterTypeEntity();
        e.setTypeCode(code);
        e.setDisplayName(name);
        e.setImageUrl("https://example.com/images/" + code.toLowerCase() + ".png");
        e.setDescription("A great scooter: " + name);
        e.setActive(active);
        e.setCreatedAt(LocalDateTime.now().minusDays(7));
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }

    // ─── listActiveScooterTypes ───────────────────────────────────────────

    @Nested
    @DisplayName("listActiveScooterTypes")
    class ListActiveScooterTypes {

        @Test
        @DisplayName("返回所有激活的 scooter type")
        void returnsActiveTypes() {
            when(scooterTypeMapper.selectList(any())).thenReturn(List.of(
                    buildType("ANDROMEDA", "Andromeda", 1),
                    buildType("LUNAR-LITE", "Lunar Lite", 1)
            ));

            List<ScooterTypeVo> result = scooterTypeService.listActiveScooterTypes();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTypeCode()).isEqualTo("ANDROMEDA");
            assertThat(result.get(0).getActive()).isTrue();
        }

        @Test
        @DisplayName("没有激活的类型 → 返回空列表")
        void noActiveTypes_returnsEmpty() {
            when(scooterTypeMapper.selectList(any())).thenReturn(List.of());

            assertThat(scooterTypeService.listActiveScooterTypes()).isEmpty();
        }

        @Test
        @DisplayName("Vo 字段映射正确（typeCode, displayName, imageUrl, description）")
        void voFieldsMappedCorrectly() {
            when(scooterTypeMapper.selectList(any())).thenReturn(List.of(
                    buildType("ORION-ULTRA", "Orion Ultra", 1)
            ));

            List<ScooterTypeVo> result = scooterTypeService.listActiveScooterTypes();

            ScooterTypeVo vo = result.get(0);
            assertThat(vo.getTypeCode()).isEqualTo("ORION-ULTRA");
            assertThat(vo.getDisplayName()).isEqualTo("Orion Ultra");
            assertThat(vo.getImageUrl()).isNotBlank();
            assertThat(vo.getDescription()).isNotBlank();
        }
    }

    // ─── getScooterType ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getScooterType")
    class GetScooterType {

        @Test
        @DisplayName("存在的 typeCode → 返回对应 Vo")
        void existingType_returnsVo() {
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildType("ANDROMEDA", "Andromeda", 1));

            ScooterTypeVo result = scooterTypeService.getScooterType("ANDROMEDA");

            assertThat(result.getTypeCode()).isEqualTo("ANDROMEDA");
            assertThat(result.getDisplayName()).isEqualTo("Andromeda");
        }

        @Test
        @DisplayName("typeCode 小写 → 自动 normalize 为大写后查询")
        void lowercaseTypeCode_normalizedToUppercase() {
            when(scooterTypeMapper.selectOne(any())).thenReturn(buildType("ANDROMEDA", "Andromeda", 1));

            ScooterTypeVo result = scooterTypeService.getScooterType("andromeda"); // 传入小写

            assertThat(result.getTypeCode()).isEqualTo("ANDROMEDA");
        }

        @Test
        @DisplayName("typeCode 不存在 → 抛 BusinessException")
        void notFound_throws() {
            when(scooterTypeMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> scooterTypeService.getScooterType("NONEXISTENT"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── listAllScooterTypes ──────────────────────────────────────────────

    @Nested
    @DisplayName("listAllScooterTypes")
    class ListAllScooterTypes {

        @Test
        @DisplayName("Admin 视图包含 inactive 类型，且有 createdAt/updatedAt 字段")
        void includesInactiveTypes() {
            when(scooterTypeMapper.selectList(any())).thenReturn(List.of(
                    buildType("ACTIVE-TYPE", "Active", 1),
                    buildType("OLD-TYPE", "Old Model", 0)  // inactive
            ));

            List<AdminScooterTypeVo> result = scooterTypeService.listAllScooterTypes();

            assertThat(result).hasSize(2);
            assertThat(result.get(1).getActive()).isFalse();
            assertThat(result.get(0).getCreatedAt()).isNotNull();
            assertThat(result.get(0).getUpdatedAt()).isNotNull();
        }
    }

    // ─── createScooterType ────────────────────────────────────────────────

    @Nested
    @DisplayName("createScooterType")
    class CreateScooterType {

        @Test
        @DisplayName("正常创建 → typeCode 被转大写，active = true")
        void success_normalizesCodeAndSetsActive() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode("  nebula-family  "); // 小写 + 空格
            req.setDisplayName("Nebula Family");
            req.setImageUrl("https://example.com/nebula.png");
            req.setDescription("A family scooter");

            when(scooterTypeMapper.selectOne(any())).thenReturn(null); // 无重复
            when(scooterTypeMapper.insert(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.createScooterType(req);

            assertThat(result.getTypeCode()).isEqualTo("NEBULA-FAMILY");
            assertThat(result.getDisplayName()).isEqualTo("Nebula Family");
            assertThat(result.getActive()).isTrue();
            assertThat(result.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("typeCode 重复 → 抛 BusinessException")
        void duplicateTypeCode_throws() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode("ANDROMEDA");
            req.setDisplayName("Andromeda");
            req.setImageUrl("https://example.com/andromeda.png");
            req.setDescription(null);

            when(scooterTypeMapper.selectOne(any())).thenReturn(buildType("ANDROMEDA", "Andromeda", 1));

            assertThatThrownBy(() -> scooterTypeService.createScooterType(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("typeCode 为空 → 抛 BusinessException")
        void blankTypeCode_throws() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode("   "); // 空格
            req.setDisplayName("Some Name");
            req.setImageUrl("https://example.com/img.png");
            req.setDescription(null);

            assertThatThrownBy(() -> scooterTypeService.createScooterType(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("typeCode 为 null → 抛 BusinessException")
        void nullTypeCode_throws() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode(null);
            req.setDisplayName("Some Name");
            req.setImageUrl("https://example.com/img.png");
            req.setDescription(null);

            assertThatThrownBy(() -> scooterTypeService.createScooterType(req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("description 为 null → 不影响创建，description 保存为 null")
        void nullDescription_savedAsNull() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode("GALAXY-SEAT");
            req.setDisplayName("Galaxy Seat");
            req.setImageUrl("https://example.com/galaxy.png");
            req.setDescription(null);

            when(scooterTypeMapper.selectOne(any())).thenReturn(null);
            when(scooterTypeMapper.insert(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.createScooterType(req);

            assertThat(result.getDescription()).isNull();
        }

        @Test
        @DisplayName("description 为纯空格 → 保存为 null（trimToNull）")
        void blankDescription_savedAsNull() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode("GALAXY-SEAT");
            req.setDisplayName("Galaxy Seat");
            req.setImageUrl("https://example.com/galaxy.png");
            req.setDescription("   "); // 纯空格

            when(scooterTypeMapper.selectOne(any())).thenReturn(null);
            when(scooterTypeMapper.insert(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.createScooterType(req);

            assertThat(result.getDescription()).isNull();
        }

        @Test
        @DisplayName("创建后 insert 被调用一次")
        void success_callsInsertOnce() {
            CreateScooterTypeRequest req = new CreateScooterTypeRequest();
            req.setTypeCode("ORION-ULTRA");
            req.setDisplayName("Orion Ultra");
            req.setImageUrl("https://example.com/orion.png");
            req.setDescription("Premium model");

            when(scooterTypeMapper.selectOne(any())).thenReturn(null);
            when(scooterTypeMapper.insert(any(ScooterTypeEntity.class))).thenReturn(1);

            scooterTypeService.createScooterType(req);

            verify(scooterTypeMapper).insert(any(ScooterTypeEntity.class));
        }
    }

    // ─── updateScooterType ────────────────────────────────────────────────

    @Nested
    @DisplayName("updateScooterType")
    class UpdateScooterType {

        @Test
        @DisplayName("更新 displayName → 新名称生效")
        void updateDisplayName_succeeds() {
            ScooterTypeEntity entity = buildType("ANDROMEDA", "Old Name", 1);
            UpdateScooterTypeRequest req = new UpdateScooterTypeRequest();
            req.setDisplayName("Andromeda Pro");

            when(scooterTypeMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.updateById(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.updateScooterType("ANDROMEDA", req);

            assertThat(result.getDisplayName()).isEqualTo("Andromeda Pro");
        }

        @Test
        @DisplayName("更新 imageUrl → 新地址生效")
        void updateImageUrl_succeeds() {
            ScooterTypeEntity entity = buildType("ANDROMEDA", "Andromeda", 1);
            UpdateScooterTypeRequest req = new UpdateScooterTypeRequest();
            req.setImageUrl("https://example.com/new-andromeda.png");

            when(scooterTypeMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.updateById(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.updateScooterType("ANDROMEDA", req);

            assertThat(result.getImageUrl()).isEqualTo("https://example.com/new-andromeda.png");
        }

        @Test
        @DisplayName("request 为 null → 抛 BusinessException")
        void nullRequest_throws() {
            assertThatThrownBy(() -> scooterTypeService.updateScooterType("ANDROMEDA", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("request 无任何字段 → 抛 BusinessException")
        void emptyRequest_throws() {
            UpdateScooterTypeRequest req = new UpdateScooterTypeRequest(); // 所有字段为 null

            assertThatThrownBy(() -> scooterTypeService.updateScooterType("ANDROMEDA", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("typeCode 不存在 → 抛 BusinessException")
        void notFound_throws() {
            UpdateScooterTypeRequest req = new UpdateScooterTypeRequest();
            req.setDisplayName("New Name");

            when(scooterTypeMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> scooterTypeService.updateScooterType("NONEXISTENT", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("typeCode 传入小写 → normalize 后能正常找到并更新")
        void lowercaseTypeCode_normalizedBeforeQuery() {
            ScooterTypeEntity entity = buildType("ANDROMEDA", "Andromeda", 1);
            UpdateScooterTypeRequest req = new UpdateScooterTypeRequest();
            req.setDisplayName("Andromeda V2");

            when(scooterTypeMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.updateById(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.updateScooterType("andromeda", req); // 小写

            assertThat(result.getDisplayName()).isEqualTo("Andromeda V2");
        }
    }

    // ─── disableScooterType ───────────────────────────────────────────────

    @Nested
    @DisplayName("disableScooterType")
    class DisableScooterType {

        @Test
        @DisplayName("正常禁用 → active 变为 false")
        void success_setsActiveToFalse() {
            ScooterTypeEntity entity = buildType("ANDROMEDA", "Andromeda", 1);

            when(scooterTypeMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.updateById(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.disableScooterType("ANDROMEDA");

            assertThat(result.getActive()).isFalse();
        }

        @Test
        @DisplayName("typeCode 不存在 → 抛 BusinessException")
        void notFound_throws() {
            when(scooterTypeMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> scooterTypeService.disableScooterType("NONEXISTENT"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("禁用后 updatedAt 已更新（比旧值新）")
        void success_updatesTimestamp() {
            LocalDateTime oldUpdatedAt = LocalDateTime.now().minusHours(1);
            ScooterTypeEntity entity = buildType("ANDROMEDA", "Andromeda", 1);
            entity.setUpdatedAt(oldUpdatedAt);

            when(scooterTypeMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.updateById(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.disableScooterType("ANDROMEDA");

            assertThat(result.getUpdatedAt()).isAfter(oldUpdatedAt);
        }

        @Test
        @DisplayName("对已经是 inactive 的类型再次禁用 → 操作幂等，不抛异常")
        void alreadyDisabled_stillSucceeds() {
            ScooterTypeEntity entity = buildType("OLD-TYPE", "Old Model", 0); // 已经 inactive

            when(scooterTypeMapper.selectOne(any())).thenReturn(entity);
            when(scooterTypeMapper.updateById(any(ScooterTypeEntity.class))).thenReturn(1);

            AdminScooterTypeVo result = scooterTypeService.disableScooterType("OLD-TYPE");

            assertThat(result.getActive()).isFalse();
        }
    }
}

package com.lcyhz.urbanova.service.impl;

import com.lcyhz.urbanova.common.exception.BusinessException;
import com.lcyhz.urbanova.domain.DomainConstants;
import com.lcyhz.urbanova.dto.booking.CancelBookingRequest;
import com.lcyhz.urbanova.dto.booking.CreateBookingRequest;
import com.lcyhz.urbanova.dto.booking.UpdateBookingRequest;
import com.lcyhz.urbanova.entity.BookingConfirmationEntity;
import com.lcyhz.urbanova.entity.BookingEntity;
import com.lcyhz.urbanova.entity.HireOptionEntity;
import com.lcyhz.urbanova.entity.ScooterEntity;
import com.lcyhz.urbanova.entity.UserEntity;
import com.lcyhz.urbanova.mapper.BookingConfirmationMapper;
import com.lcyhz.urbanova.mapper.BookingMapper;
import com.lcyhz.urbanova.mapper.HireOptionMapper;
import com.lcyhz.urbanova.mapper.PaymentMapper;
import com.lcyhz.urbanova.mapper.ScooterMapper;
import com.lcyhz.urbanova.mapper.UserMapper;
import com.lcyhz.urbanova.service.DiscountRuleService;
import com.lcyhz.urbanova.service.ScooterService;
import com.lcyhz.urbanova.service.DiscountRuleService.DiscountComputation;
import com.lcyhz.urbanova.service.support.EmailDeliveryService;
import com.lcyhz.urbanova.service.support.PlatformSupportService;
import com.lcyhz.urbanova.vo.booking.BookingDetailVo;
import com.lcyhz.urbanova.vo.booking.BookingListItemVo;
import com.lcyhz.urbanova.vo.booking.CancelBookingVo;
import com.lcyhz.urbanova.vo.booking.CreateBookingVo;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingServiceImpl Tests")
class BookingServiceImplTest {

    @Mock private BookingMapper bookingMapper;
    @Mock private HireOptionMapper hireOptionMapper;
    @Mock private ScooterMapper scooterMapper;
    @Mock private UserMapper userMapper;
    @Mock private PaymentMapper paymentMapper;
    @Mock private BookingConfirmationMapper bookingConfirmationMapper;
    @Mock private DiscountRuleService discountRuleService;
    @Mock private ScooterService scooterService;
    @Mock private PlatformSupportService platformSupportService;
    @Mock private EmailDeliveryService emailDeliveryService;

    @InjectMocks
    private BookingServiceImpl bookingService;

    // ─── Setup ───────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Inject @Value fields that Spring normally injects
        ReflectionTestUtils.setField(bookingService, "lowBatteryThreshold", 20);
        ReflectionTestUtils.setField(bookingService, "batteryDrainPerMinute", 1);
    }

    // ─── Helper builders ─────────────────────────────────────────────────

    private HireOptionEntity buildHireOption(String id, String code, int minutes, String price) {
        HireOptionEntity h = new HireOptionEntity();
        h.setHireOptionId(id);
        h.setCode(code);
        h.setDurationMinutes(minutes);
        h.setBasePrice(new BigDecimal(price));
        h.setActive(1);
        return h;
    }

    private ScooterEntity buildScooter(String scooterId, String status, int battery) {
        ScooterEntity s = new ScooterEntity();
        s.setScooterId(scooterId);
        s.setStatus(status);
        s.setBatteryPercent(battery);
        s.setBatteryUpdatedAt(LocalDateTime.now().minusMinutes(5));
        return s;
    }

    private BookingEntity buildBooking(String bookingId, String userId, String status) {
        BookingEntity b = new BookingEntity();
        b.setBookingId(bookingId);
        b.setBookingRef("REF-" + bookingId);
        b.setUserId(userId);
        b.setScooterId("SC-001");
        b.setHireOptionId("HIRE-30MIN");
        b.setStatus(status);
        b.setPaymentStatus(DomainConstants.PAYMENT_STATUS_UNPAID);
        b.setCustomerType(DomainConstants.CUSTOMER_TYPE_REGISTERED);
        b.setPriceBase(new BigDecimal("5.00"));
        b.setPriceDiscount(BigDecimal.ZERO);
        b.setPriceFinal(new BigDecimal("5.00"));
        b.setStartAt(LocalDateTime.now().plusHours(1));
        b.setEndAt(LocalDateTime.now().plusHours(2));
        b.setCreatedAt(LocalDateTime.now());
        b.setUpdatedAt(LocalDateTime.now());
        return b;
    }

    /** Returns a DiscountComputation with zero discount (no promotion applied) */
    private DiscountRuleService.DiscountComputation noDiscount(BigDecimal basePrice) {
        return new DiscountRuleService.DiscountComputation(
                BigDecimal.ZERO, BigDecimal.ZERO, basePrice,
                List.of(), List.of(), BigDecimal.ZERO);
    }

    // ─── createBooking ────────────────────────────────────────────────────

    @Nested
    @DisplayName("createBooking")
    class CreateBooking {

        @Test
        @DisplayName("正常创建 booking，返回 CreateBookingVo 且状态为 PENDING_PAYMENT")
        void success_returnsVoWithPendingPaymentStatus() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-30MIN");

            HireOptionEntity hireOption = buildHireOption("HIRE-30MIN", "30MIN", 30, "5.00");
            ScooterEntity scooter = buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80);

            when(userMapper.selectOne(any())).thenReturn(null); // user not found → skip age check
            when(hireOptionMapper.selectOne(any())).thenReturn(hireOption);
            when(scooterMapper.selectOne(any())).thenReturn(scooter);
            when(discountRuleService.calculateForUser(eq("USR-001"), any())).thenReturn(noDiscount(new BigDecimal("5.00")));
            when(scooterMapper.update(any(), any())).thenReturn(1); // reserve success
            when(bookingMapper.insert(any(BookingEntity.class))).thenReturn(1);

            CreateBookingVo result = bookingService.createBooking("USR-001", req);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.PENDING_PAYMENT);
            assertThat(result.getPaymentStatus()).isEqualTo(DomainConstants.PAYMENT_STATUS_UNPAID);
            assertThat(result.getPriceBreakdown().getBase()).isEqualByComparingTo("5.00");
            assertThat(result.getPriceBreakdown().getFinalPrice()).isEqualByComparingTo("5.00");
            verify(bookingMapper).insert(any(BookingEntity.class));
        }

        @Test
        @DisplayName("hire option 不存在 → 抛 BusinessException")
        void hireOptionNotFound_throws() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-INVALID");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookingService.createBooking("USR-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("scooter 不存在 → 抛 BusinessException")
        void scooterNotFound_throws() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-999");
            req.setHireOptionId("HIRE-30MIN");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.selectOne(any())).thenReturn(buildHireOption("HIRE-30MIN", "30MIN", 30, "5.00"));
            when(scooterMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookingService.createBooking("USR-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("scooter 状态不是 AVAILABLE（被预约）→ 抛 BusinessException")
        void scooterNotAvailable_throws() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-30MIN");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.selectOne(any())).thenReturn(buildHireOption("HIRE-30MIN", "30MIN", 30, "5.00"));
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.RESERVED, 80));

            assertThatThrownBy(() -> bookingService.createBooking("USR-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("并发竞争预约 scooter 失败（update 返回 0）→ 抛 BusinessException")
        void reserveScooterRaceLost_throws() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-30MIN");

            when(userMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.selectOne(any())).thenReturn(buildHireOption("HIRE-30MIN", "30MIN", 30, "5.00"));
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80));
            when(discountRuleService.calculateForUser(any(), any())).thenReturn(noDiscount(new BigDecimal("5.00")));
            when(scooterMapper.update(any(), any())).thenReturn(0); // 竞争失败

            assertThatThrownBy(() -> bookingService.createBooking("USR-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("未成年用户（8岁）→ 抛 BusinessException")
        void underageRider_throws() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-30MIN");

            UserEntity youngUser = new UserEntity();
            youngUser.setUserId("USR-YOUNG");
            youngUser.setBirthDate(LocalDate.now().minusYears(8)); // 8岁，未达最低骑行年龄

            when(userMapper.selectOne(any())).thenReturn(youngUser);

            assertThatThrownBy(() -> bookingService.createBooking("USR-YOUNG", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("成年用户（25岁）→ 正常通过年龄检查")
        void adultRider_passesAgeCheck() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-30MIN");

            UserEntity adultUser = new UserEntity();
            adultUser.setUserId("USR-001");
            adultUser.setBirthDate(LocalDate.now().minusYears(25)); // 25岁

            when(userMapper.selectOne(any())).thenReturn(adultUser);
            when(hireOptionMapper.selectOne(any())).thenReturn(buildHireOption("HIRE-30MIN", "30MIN", 30, "5.00"));
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80));
            when(discountRuleService.calculateForUser(any(), any())).thenReturn(noDiscount(new BigDecimal("5.00")));
            when(scooterMapper.update(any(), any())).thenReturn(1);
            when(bookingMapper.insert(any(BookingEntity.class))).thenReturn(1);

            CreateBookingVo result = bookingService.createBooking("USR-001", req);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.PENDING_PAYMENT);
        }

        @Test
        @DisplayName("有折扣时，priceBreakdown 正确反映折扣后的金额")
        void withDiscount_priceBreakdownReflectsDiscount() {
            CreateBookingRequest req = new CreateBookingRequest();
            req.setScooterId("SC-001");
            req.setHireOptionId("HIRE-30MIN");

            HireOptionEntity hireOption = buildHireOption("HIRE-30MIN", "30MIN", 30, "10.00");

            DiscountRuleService.DiscountComputation discountedComputation =
                    new DiscountRuleService.DiscountComputation(
                            BigDecimal.ZERO,
                            new BigDecimal("1.00"), // 折扣额
                            new BigDecimal("9.00"), // 最终价格
                            List.of(), List.of(),
                            new BigDecimal("10.00"));

            when(userMapper.selectOne(any())).thenReturn(null);
            when(hireOptionMapper.selectOne(any())).thenReturn(hireOption);
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.AVAILABLE, 80));
            when(discountRuleService.calculateForUser(any(), any())).thenReturn(discountedComputation);
            when(scooterMapper.update(any(), any())).thenReturn(1);
            when(bookingMapper.insert(any(BookingEntity.class))).thenReturn(1);

            CreateBookingVo result = bookingService.createBooking("USR-001", req);

            assertThat(result.getPriceBreakdown().getBase()).isEqualByComparingTo("10.00");
            assertThat(result.getPriceBreakdown().getDiscount()).isEqualByComparingTo("1.00");
            assertThat(result.getPriceBreakdown().getFinalPrice()).isEqualByComparingTo("9.00");
        }
    }

    // ─── cancelBooking ────────────────────────────────────────────────────

    @Nested
    @DisplayName("cancelBooking")
    class CancelBooking {

        @Test
        @DisplayName("PENDING_PAYMENT 状态可以取消，返回 CANCELLED 状态")
        void pendingPayment_cancels() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.PENDING_PAYMENT);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.RESERVED, 80));
            when(scooterMapper.update(any(), any())).thenReturn(1);

            CancelBookingVo result = bookingService.cancelBooking("USR-001", "BKG-001", null);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.CANCELLED);
            assertThat(result.getBookingId()).isEqualTo("BKG-001");
            assertThat(result.getCancelledAt()).isNotNull();
        }

        @Test
        @DisplayName("CONFIRMED 状态可以取消")
        void confirmed_cancels() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.RESERVED, 80));
            when(scooterMapper.update(any(), any())).thenReturn(1);

            CancelBookingVo result = bookingService.cancelBooking("USR-001", "BKG-001", null);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("已经是 CANCELLED 状态 → 幂等返回，不再写数据库")
        void alreadyCancelled_idempotent() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CANCELLED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            CancelBookingVo result = bookingService.cancelBooking("USR-001", "BKG-001", null);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.CANCELLED);
            verify(bookingMapper, never()).updateById(any(BookingEntity.class)); // 不应写库
        }

        @Test
        @DisplayName("ACTIVE 状态不可取消 → 抛 BusinessException")
        void activeStatus_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.ACTIVE);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            assertThatThrownBy(() -> bookingService.cancelBooking("USR-001", "BKG-001", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("COMPLETED 状态不可取消 → 抛 BusinessException")
        void completedStatus_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.COMPLETED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            assertThatThrownBy(() -> bookingService.cancelBooking("USR-001", "BKG-001", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("访问别人的 booking → 抛 BusinessException (FORBIDDEN)")
        void otherUsersBooking_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-OWNER", DomainConstants.BookingStatus.PENDING_PAYMENT);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            assertThatThrownBy(() -> bookingService.cancelBooking("USR-OTHER", "BKG-001", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("传入取消原因 → 原因正确写入")
        void withCancelReason_setsReason() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CONFIRMED);
            CancelBookingRequest req = new CancelBookingRequest();
            req.setReason("Changed my plans");

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);
            when(scooterMapper.selectOne(any())).thenReturn(buildScooter("SC-001", DomainConstants.ScooterStatus.RESERVED, 80));
            when(scooterMapper.update(any(), any())).thenReturn(1);

            CancelBookingVo result = bookingService.cancelBooking("USR-001", "BKG-001", req);

            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.CANCELLED);
        }

        @Test
        @DisplayName("booking 不存在 → 抛 BusinessException")
        void bookingNotFound_throws() {
            when(bookingMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookingService.cancelBooking("USR-001", "BKG-NONE", null))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── startBooking ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("startBooking")
    class StartBooking {

        @Test
        @DisplayName("CONFIRMED 状态可以开始骑行 → 状态变 ACTIVE")
        void confirmed_startsSuccessfully() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(userMapper.selectOne(any())).thenReturn(null);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);
            when(scooterMapper.update(any(), any())).thenReturn(1);

            Map<String, Object> result = bookingService.startBooking("USR-001", DomainConstants.ROLE_CUSTOMER, "BKG-001");

            assertThat(result).containsEntry("status", DomainConstants.BookingStatus.ACTIVE);
        }

        @Test
        @DisplayName("PENDING_PAYMENT 状态不能开始 → 抛 BusinessException")
        void pendingPayment_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.PENDING_PAYMENT);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(userMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookingService.startBooking("USR-001", DomainConstants.ROLE_CUSTOMER, "BKG-001"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("ACTIVE 状态再次开始 → 抛 BusinessException")
        void alreadyActive_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.ACTIVE);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(userMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookingService.startBooking("USR-001", DomainConstants.ROLE_CUSTOMER, "BKG-001"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("MANAGER 角色可以操作任意用户的 booking（绕过权限校验）")
        void managerRole_canAccessAnyBooking() {
            BookingEntity booking = buildBooking("BKG-001", "USR-OTHER", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(userMapper.selectOne(any())).thenReturn(null);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);
            when(scooterMapper.update(any(), any())).thenReturn(1);

            Map<String, Object> result = bookingService.startBooking("USR-MANAGER", DomainConstants.ROLE_MANAGER, "BKG-001");

            assertThat(result).containsEntry("status", DomainConstants.BookingStatus.ACTIVE);
        }

        @Test
        @DisplayName("STAFF 角色可以操作任意用户的 booking")
        void staffRole_canAccessAnyBooking() {
            BookingEntity booking = buildBooking("BKG-001", "USR-OTHER", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(userMapper.selectOne(any())).thenReturn(null);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);
            when(scooterMapper.update(any(), any())).thenReturn(1);

            Map<String, Object> result = bookingService.startBooking("USR-STAFF", DomainConstants.ROLE_STAFF, "BKG-001");

            assertThat(result).containsEntry("status", DomainConstants.BookingStatus.ACTIVE);
        }
    }

    // ─── endBooking ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("endBooking")
    class EndBooking {

        @Test
        @DisplayName("ACTIVE 状态可以结束骑行 → 状态变 COMPLETED")
        void active_endsSuccessfully() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.ACTIVE);
            ScooterEntity scooter = buildScooter("SC-001", DomainConstants.ScooterStatus.IN_USE, 70);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(scooterMapper.selectOne(any())).thenReturn(scooter);
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);
            when(scooterMapper.update(any(), any())).thenReturn(1);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);

            Map<String, Object> result = bookingService.endBooking("USR-001", DomainConstants.ROLE_CUSTOMER, "BKG-001");

            assertThat(result).containsEntry("status", DomainConstants.BookingStatus.COMPLETED);
        }

        @Test
        @DisplayName("非 ACTIVE 状态不能结束 → 抛 BusinessException")
        void notActive_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            assertThatThrownBy(() -> bookingService.endBooking("USR-001", DomainConstants.ROLE_CUSTOMER, "BKG-001"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("结束时电量低于阈值 → scooter 状态变为 LOW_BATTERY")
        void endBooking_lowBattery_scooterSetToLowBattery() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.ACTIVE);
            // 电量 10% < 20% 阈值
            ScooterEntity scooter = buildScooter("SC-001", DomainConstants.ScooterStatus.IN_USE, 10);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(scooterMapper.selectOne(any())).thenReturn(scooter);
            when(scooterMapper.updateById(any(ScooterEntity.class))).thenReturn(1);
            when(scooterMapper.update(any(), any())).thenReturn(1);
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);

            Map<String, Object> result = bookingService.endBooking("USR-001", DomainConstants.ROLE_CUSTOMER, "BKG-001");

            assertThat(result).containsEntry("status", DomainConstants.BookingStatus.COMPLETED);
            // scooterMapper.update is called, verifying it was invoked (status change to LOW_BATTERY)
            verify(scooterMapper).update(any(), any());
        }
    }

    // ─── listBookings ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("listBookings")
    class ListBookings {

        @Test
        @DisplayName("不传 status 过滤 → 返回该用户所有 booking")
        void noFilter_returnsAll() {
            String userId = "USR-001";
            when(bookingMapper.selectList(any())).thenReturn(List.of(
                    buildBooking("BKG-001", userId, DomainConstants.BookingStatus.CONFIRMED),
                    buildBooking("BKG-002", userId, DomainConstants.BookingStatus.COMPLETED)
            ));

            List<BookingListItemVo> result = bookingService.listBookings(userId, null);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("传入 status 过滤 → 只返回对应状态的 booking")
        void withStatusFilter_returnsFiltered() {
            String userId = "USR-001";
            when(bookingMapper.selectList(any())).thenReturn(List.of(
                    buildBooking("BKG-001", userId, DomainConstants.BookingStatus.CONFIRMED)
            ));

            List<BookingListItemVo> result = bookingService.listBookings(userId, "confirmed");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(DomainConstants.BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("没有任何 booking → 返回空列表")
        void noBookings_returnsEmpty() {
            when(bookingMapper.selectList(any())).thenReturn(List.of());

            List<BookingListItemVo> result = bookingService.listBookings("USR-001", null);

            assertThat(result).isEmpty();
        }
    }

    // ─── getBookingDetail ─────────────────────────────────────────────────

    @Nested
    @DisplayName("getBookingDetail")
    class GetBookingDetail {

        @Test
        @DisplayName("本人查询自己的 booking → 返回详情")
        void ownBooking_returnsDetail() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            BookingDetailVo result = bookingService.getBookingDetail("USR-001", "BKG-001");

            assertThat(result.getBookingId()).isEqualTo("BKG-001");
            assertThat(result.getUserId()).isEqualTo("USR-001");
            assertThat(result.getStatus()).isEqualTo(DomainConstants.BookingStatus.CONFIRMED);
        }

        @Test
        @DisplayName("booking 不存在 → 抛 BusinessException")
        void notFound_throws() {
            when(bookingMapper.selectOne(any())).thenReturn(null);

            assertThatThrownBy(() -> bookingService.getBookingDetail("USR-001", "BKG-NONE"))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("查询他人的 booking（非 manager/staff）→ 抛 BusinessException")
        void forbiddenUser_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-OWNER", DomainConstants.BookingStatus.CONFIRMED);

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            assertThatThrownBy(() -> bookingService.getBookingDetail("USR-OTHER", "BKG-001"))
                    .isInstanceOf(BusinessException.class);
        }
    }

    // ─── updateBooking ────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateBooking")
    class UpdateBooking {

        @Test
        @DisplayName("request 为 null → 抛 BusinessException")
        void nullRequest_throws() {
            assertThatThrownBy(() -> bookingService.updateBooking("USR-001", "BKG-001", null))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("request 无任何字段 → 抛 BusinessException")
        void emptyRequest_throws() {
            UpdateBookingRequest req = new UpdateBookingRequest(); // 所有字段为 null

            assertThatThrownBy(() -> bookingService.updateBooking("USR-001", "BKG-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("ACTIVE 状态不可更新（非 mutable 状态）→ 抛 BusinessException")
        void activestatus_throws() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.ACTIVE);
            UpdateBookingRequest req = new UpdateBookingRequest();
            req.setHireOptionId("HIRE-60MIN");

            when(bookingMapper.selectOne(any())).thenReturn(booking);

            assertThatThrownBy(() -> bookingService.updateBooking("USR-001", "BKG-001", req))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("PENDING_PAYMENT 状态可以更换 hire option → 返回更新后的详情")
        void pendingPayment_changeHireOption_succeeds() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.PENDING_PAYMENT);
            HireOptionEntity newOption = buildHireOption("HIRE-60MIN", "60MIN", 60, "8.00");

            UpdateBookingRequest req = new UpdateBookingRequest();
            req.setHireOptionId("HIRE-60MIN");

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(hireOptionMapper.selectOne(any())).thenReturn(newOption);
            when(discountRuleService.calculateForUser(any(), any())).thenReturn(noDiscount(new BigDecimal("8.00")));
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);

            BookingDetailVo result = bookingService.updateBooking("USR-001", "BKG-001", req);

            assertThat(result).isNotNull();
            assertThat(result.getHireOptionId()).isEqualTo("HIRE-60MIN");
        }

        @Test
        @DisplayName("更新开始时间 → endAt 随 hireOption duration 正确计算")
        void changeStartAt_updatesEndAt() {
            BookingEntity booking = buildBooking("BKG-001", "USR-001", DomainConstants.BookingStatus.CONFIRMED);
            HireOptionEntity existingOption = buildHireOption("HIRE-30MIN", "30MIN", 30, "5.00");

            LocalDateTime newStartAt = LocalDateTime.now().plusDays(1);
            UpdateBookingRequest req = new UpdateBookingRequest();
            req.setPlannedStartAt(newStartAt);

            when(bookingMapper.selectOne(any())).thenReturn(booking);
            when(hireOptionMapper.selectOne(any())).thenReturn(existingOption);
            when(discountRuleService.calculateForUser(any(), any())).thenReturn(noDiscount(new BigDecimal("5.00")));
            when(bookingMapper.updateById(any(BookingEntity.class))).thenReturn(1);

            BookingDetailVo result = bookingService.updateBooking("USR-001", "BKG-001", req);

            assertThat(result.getStartAt()).isEqualTo(newStartAt);
            assertThat(result.getEndAt()).isEqualTo(newStartAt.plusMinutes(30));
        }
    }
}

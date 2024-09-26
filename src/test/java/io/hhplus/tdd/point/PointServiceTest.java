package io.hhplus.tdd.test;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PointServiceTest {

    private static final Logger log = LoggerFactory.getLogger(PointServiceTest.class);

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    @Mock
    private UserValidate userValidate;

    @Mock
    private PointValidate pointValidate;

    private PointService pointService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pointService = new PointService(userPointTable, pointHistoryTable, userValidate, pointValidate);
    }

    @Test
    @DisplayName("사용자 포인트 조회 테스트 - 성공")
    void testGetUserPoint() {
        long userId = 1L;
        UserPoint userPoint = new UserPoint(userId, 1000, System.currentTimeMillis());

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        doNothing().when(userValidate).validateUser(userId);

        log.info("Testing getUserPoint for userId: {}", userId);
        UserPoint result = pointService.getUserPoint(userId);

        assertEquals(userPoint, result, "사용자 포인트가 일치해야 합니다");
        verify(userPointTable).selectById(userId);
        verify(userValidate).validateUser(userId);
    }

    @Test
    @DisplayName("포인트 충전 테스트 - 성공")
    void testChargeUserPointSuccess() {
        long userId = 1L;
        long chargeAmount = 500L;
        long currentTime = System.currentTimeMillis();

        when(userPointTable.insertOrUpdate(userId, chargeAmount))
                .thenReturn(new UserPoint(userId, 500, currentTime));
        doNothing().when(pointValidate).validateChargeAmount(chargeAmount);
        doNothing().when(userValidate).validateUser(userId);

        log.info("Charging user {} with amount {}", userId, chargeAmount);
        UserPoint updatedPoint = pointService.chargeUserPoint(userId, chargeAmount);

        assertEquals(500L, updatedPoint.point(), "충전 후 포인트는 500이어야 합니다");
        verify(pointHistoryTable).insert(userId, chargeAmount, TransactionType.CHARGE, anyLong());
        verify(pointValidate).validateChargeAmount(chargeAmount);
        verify(userValidate).validateUser(userId);
    }

    @Test
    @DisplayName("포인트 충전 테스트 - 실패 (잘못된 금액)")
    void testChargeUserPointFail() {
        long userId = 1L;
        long chargeAmount = -500L;

        doThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."))
                .when(pointValidate).validateChargeAmount(chargeAmount);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.chargeUserPoint(userId, chargeAmount);
        });

        assertEquals("충전 금액은 0보다 커야 합니다.", exception.getMessage());
        verify(pointHistoryTable, never()).insert(anyLong(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("유저 검증 테스트 - 실패")
    void testValidateUserFail() {
        long invalidUserId = -1L;

        doThrow(new IllegalArgumentException("유효하지 않은 유저입니다."))
                .when(userValidate).validateUser(invalidUserId);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            pointService.getUserPoint(invalidUserId);
        });

        assertEquals("유효하지 않은 유저입니다.", exception.getMessage());
        verify(userPointTable, never()).selectById(invalidUserId);
    }
}
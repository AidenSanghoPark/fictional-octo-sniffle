package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        pointService = new PointService(userPointTable, pointHistoryTable, userValidate, pointValidate);
//        log.info("PointService 테스트 설정 완료");
    }

    @Test
    @DisplayName("사용자 포인트 조회 테스트")
    void testGetUserPoint() {
        long userId = 1L;
        UserPoint expectedPoint = new UserPoint(userId, 1000, System.currentTimeMillis());

        log.info("사용자 포인트 조회 테스트 시작: userId={}", userId);
        when(userPointTable.selectById(userId)).thenReturn(expectedPoint);

        UserPoint result = pointService.getUserPoint(userId);

        log.info("조회된 사용자 포인트: {}", result);
        assertEquals(expectedPoint, result);
        verify(userValidate).validateUser(userId);
        verify(userPointTable).selectById(userId);
        log.info("사용자 포인트 조회 테스트 완료");
    }

    @Test
    @DisplayName("포인트 내역 조회 테스트")
    void testGetPointHistories() {
        long userId = 1L;
        List<PointHistory> expectedHistories = Arrays.asList(
                new PointHistory(1L, userId, 500, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, userId, 300, TransactionType.USE, System.currentTimeMillis())
        );

        log.info("포인트 내역 조회 테스트 시작: userId={}", userId);
        when(pointHistoryTable.selectAllByUserId(userId)).thenReturn(expectedHistories);

        List<PointHistory> result = pointService.getPointHistories(userId);

        log.info("조회된 포인트 내역: {}", result);
        assertEquals(expectedHistories, result);
        verify(userValidate).validateUser(userId);
        verify(pointHistoryTable).selectAllByUserId(userId);
        log.info("포인트 내역 조회 테스트 완료");
    }

    @Test
    @DisplayName("포인트 충전 테스트")
    void testChargeUserPoint() {
        long userId = 1L;
        long amount = 500L;
        UserPoint updatedPoint = new UserPoint(userId, 1500, System.currentTimeMillis());

        log.info("포인트 충전 테스트 시작: userId={}, chargeAmount={}", userId, amount);
        when(userPointTable.insertOrUpdate(userId, amount)).thenReturn(updatedPoint);

        UserPoint result = pointService.chargeUserPoint(userId, amount);

        log.info("충전 후 사용자 포인트: {}", result);
        assertEquals(updatedPoint, result);
        verify(userValidate).validateUser(userId);
        verify(pointValidate).validateChargeAmount(amount);
        verify(userPointTable).insertOrUpdate(userId, amount);
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.CHARGE), anyLong());
        log.info("포인트 충전 테스트 완료");
    }

    @Test
    @DisplayName("동시에 여러 번 포인트 충전 테스트")
    void testConcurrentPointCharge() throws InterruptedException {
        int numberOfThreads = 10;
        long userId = 1L;
        long chargeAmount = 100L;
        AtomicLong totalCharged = new AtomicLong(0);

        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        when(userPointTable.insertOrUpdate(eq(userId), anyLong())).thenAnswer(invocation -> {
            long amount = invocation.getArgument(1);
            long newTotal = totalCharged.addAndGet(amount);
            return new UserPoint(userId, newTotal, System.currentTimeMillis());
        });


        // 여러 스레드에서 동시에 충전 실행
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    pointService.chargeUserPoint(userId, chargeAmount);
                    log.info("아이디: {}충전금액: {} 잔고: {}", userId, chargeAmount, totalCharged.get());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 작업을 마칠 때까지 대기
        executorService.shutdown();

        // 검증
        long expectedTotalCharge = numberOfThreads * chargeAmount;
        assertEquals(expectedTotalCharge, totalCharged.get(), "총 충전된 금액이 예상과 일치해야 합니다.");

        verify(userValidate, times(numberOfThreads)).validateUser(userId);
        verify(pointValidate, times(numberOfThreads)).validateChargeAmount(chargeAmount);
        verify(userPointTable, times(numberOfThreads)).insertOrUpdate(eq(userId), anyLong());
        verify(pointHistoryTable, times(numberOfThreads)).insert(eq(userId), eq(chargeAmount), eq(TransactionType.CHARGE), anyLong());

        log.info("동시성 테스트 완료: 총 충전 금액 = {}", totalCharged.get());
    }


    @Test
    @DisplayName("포인트 사용 테스트")
    void testUseUserPoint() {
        long userId = 1L;
        long amount = 300L;
        UserPoint updatedPoint = new UserPoint(userId, 700, System.currentTimeMillis());

        log.info("포인트 사용 테스트 시작: userId={}, useAmount={}", userId, amount);
        when(userPointTable.insertOrUpdate(userId, -amount)).thenReturn(updatedPoint);

        UserPoint result = pointService.useUserPoint(userId, amount);

        log.info("사용 후 사용자 포인트: {}", result);
        assertEquals(updatedPoint, result);
        verify(userValidate).validateUser(userId);
        verify(pointValidate).validateUseAmount(userId, amount);
        verify(userPointTable).insertOrUpdate(userId, -amount);
        verify(pointHistoryTable).insert(eq(userId), eq(amount), eq(TransactionType.USE), anyLong());
        log.info("포인트 사용 테스트 완료");
    }

    @Test
    @DisplayName("잘못된 사용자 ID로 포인트 조회 시 예외 발생")
    void testGetUserPointWithInvalidUserId() {
        long invalidUserId = -1L;

        log.info("잘못된 사용자 ID 테스트 시작: invalidUserId={}", invalidUserId);
        doThrow(new IllegalArgumentException("유효하지 않은 사용자 ID입니다."))
                .when(userValidate).validateUser(invalidUserId);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> pointService.getUserPoint(invalidUserId));
        log.error("예외 발생: {}", exception.getMessage());

        assertEquals("유효하지 않은 사용자 ID입니다.", exception.getMessage());
        log.info("잘못된 사용자 ID 테스트 완료");
    }

    @Test
    @DisplayName("잘못된 충전 금액으로 포인트 충전 시 예외 발생")
    void testChargeUserPointWithInvalidAmount() {
        long userId = 1L;
        long invalidAmount = -500L;

        log.info("잘못된 충전 금액 테스트 시작: userId={}, invalidAmount={}", userId, invalidAmount);
        doThrow(new IllegalArgumentException("충전 금액은 0보다 커야 합니다."))
                .when(pointValidate).validateChargeAmount(invalidAmount);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> pointService.chargeUserPoint(userId, invalidAmount));
        log.error("예외 발생: {}", exception.getMessage());

        assertEquals("충전 금액은 0보다 커야 합니다.", exception.getMessage());
        log.info("잘못된 충전 금액 테스트 완료");
    }

    @Test
    @DisplayName("잔액 부족으로 포인트 사용 실패 시 예외 발생")
    void testUseUserPointWithInsufficientBalance() {
        long userId = 1L;
        long amount = 2000L;

        log.info("잔액 부족 테스트 시작: userId={}, amount={}", userId, amount);
        doThrow(new IllegalArgumentException("포인트가 부족합니다."))
                .when(pointValidate).validateUseAmount(userId, amount);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> pointService.useUserPoint(userId, amount));
        log.error("예외 발생: {}", exception.getMessage());

        assertEquals("포인트가 부족합니다.", exception.getMessage());
        log.info("잔액 부족 테스트 완료");
    }
}
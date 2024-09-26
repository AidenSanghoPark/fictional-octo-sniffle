package io.hhplus.tdd.point;
import io.hhplus.tdd.database.UserPointTable;

public class PointValidate {

    private final UserPointTable userPointTable;

    public PointValidate(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    // 충전 금액 검증
    public void validateChargeAmount(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
    }

    // 사용 금액 검증
    public void validateUseAmount(long userId,long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        // 현재 유저 포인트 조회
        UserPoint currentUserPoint = userPointTable.selectById(userId);
        if (currentUserPoint == null || currentUserPoint.point() < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
    }
}

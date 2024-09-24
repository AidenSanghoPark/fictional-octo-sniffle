package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.point.TransactionType;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    // 특정 유저의 포인트 조회
    public UserPoint getUserPoint(long userId) {
        return userPointTable.selectById(userId);
    }

    // 특정 유저의 포인트 내역 조회
    public List<PointHistory> getPointHistories(long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    // 특정 유저의 포인트 충전
    public UserPoint chargeUserPoint(long userId, long amount) {
        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, amount);
        pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
        return updatedUserPoint;
    }

    // 특정 유저의 포인트 사용
    public UserPoint useUserPoint(long userId, long amount) {
        UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, -amount);
        pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
        return updatedUserPoint;
    }
}

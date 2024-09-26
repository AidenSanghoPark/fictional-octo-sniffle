    package io.hhplus.tdd.point;

    import io.hhplus.tdd.database.UserPointTable;
    import io.hhplus.tdd.database.PointHistoryTable;
    import org.springframework.stereotype.Service;
    import org.springframework.transaction.annotation.Transactional;
    import java.util.List;
    import java.util.concurrent.ConcurrentHashMap;
    import java.util.concurrent.locks.Lock;
    import java.util.concurrent.locks.ReentrantLock;

    @Service
    public class PointService {

        private final UserPointTable userPointTable;
        private final PointHistoryTable pointHistoryTable;
        private final UserValidate userValidate;
        private final PointValidate pointValidate;
        private final ConcurrentHashMap<Long, Lock> userLocks = new ConcurrentHashMap<>();

        public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable, UserValidate userValidate, PointValidate pointValidate) {
            this.userPointTable = userPointTable;
            this.pointHistoryTable = pointHistoryTable;
            this.userValidate = userValidate;
            this.pointValidate = pointValidate;
        }

        // 특정 아이디에 Lock이 있으면 Lock 반환, 없으면 새로 생성해서 반환
        private Lock getUserLock(long userId) {
            // k는 맵에서 찾지못한 userId를 나타내며
            //return userLocks.computeIfAbsent(userId, _ -> new ReentrantLock()); 와 같이 표현할 수 도 있다.
            //Map<String, User> userCache = new ConcurrentHashMap<>();
            //User user = userCache.computeIfAbsent(userId, k -> loadUserFromDatabase(k)); 이와 같이도 쓸 수 있음
            return userLocks.computeIfAbsent(userId, k -> new ReentrantLock());
        }

        // 특정 유저의 포인트 조회
        @Transactional(readOnly = true)
        public UserPoint getUserPoint(long userId) {
            userValidate.validateUser(userId);
            return userPointTable.selectById(userId);
        }

        // 특정 유저의 포인트 내역 조회
        @Transactional(readOnly = true)
        public List<PointHistory> getPointHistories(long userId) {
            userValidate.validateUser(userId);
            return pointHistoryTable.selectAllByUserId(userId);
        }

        // 특정 유저의 포인트 충전
        @Transactional
        public UserPoint chargeUserPoint(long userId, long amount) {
            // 해당 아이디에 lock
            Lock lock = getUserLock(userId);
            lock.lock();
            try {
                // 유저 검증
                userValidate.validateUser(userId);
                // 충전 금액 검증
                pointValidate.validateChargeAmount(amount);
                // 포인트 충전
                UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, amount);
                pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
                return updatedUserPoint;
            } finally {
                lock.unlock();
            }
        }

        // 특정 유저의 포인트 사용
        @Transactional
        public UserPoint useUserPoint(long userId, long amount) {
            Lock lock = getUserLock(userId);
            lock.lock();
            try {
                // 유저 검증
                userValidate.validateUser(userId);
                // 사용 금액 검증
                pointValidate.validateUseAmount(userId, amount);

                // 포인트 사용
                UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, -amount);
                pointHistoryTable.insert(userId, amount, TransactionType.USE, System.currentTimeMillis());
                return updatedUserPoint;
            } finally {
              lock.unlock();
            }
        }

    }

# PointService 동시성 제어 방식 분석

## 개요
`PointService` 클래스는 사용자 포인트 충전 및 사용과 관련된 기능을 제공하는 서비스입니다. 이 클래스는 동시성 문제를 해결하기 위해 락(Lock)과 동시성 컬렉션(ConcurrentHashMap)을 사용하여 스레드 안전성을 보장합니다.

## 동시성 제어 방식

### 1. ConcurrentHashMap
- `ConcurrentHashMap<Long, Lock> userLocks`는 사용자 ID에 대한 락을 저장하는 데 사용됩니다. 이 맵은 여러 스레드가 동시에 접근할 수 있도록 설계되어 있어 스레드 안전성을 보장합니다.
- 사용자 ID를 키로 사용하여 해당 사용자에 대한 락을 관리합니다.

### 2. ReentrantLock
- `ReentrantLock` 클래스는 Java의 락 구현 중 하나로, 같은 스레드가 여러 번 락을 획득할 수 있는 기능을 제공합니다. 이로 인해 락의 재진입이 가능해집니다.
- `getUserLock(long userId)` 메서드는 사용자 ID에 대해 존재하는 락을 반환하거나, 없을 경우 새로 생성하여 반환합니다. 이 방식은 락의 중복 생성을 방지하여 메모리 사용을 최적화합니다.

### 3. Lock 사용
- `chargeUserPoint` 및 `useUserPoint` 메서드에서는 특정 사용자에 대한 락을 획득한 후, 해당 사용자에 대한 포인트 충전 또는 사용 로직을 수행합니다.
- 각각의 메서드는 다음과 같은 방식으로 동작합니다:
    1. 사용자 ID에 대한 락을 획득합니다.
    2. 사용자 검증을 통해 유효성을 검사합니다.
    3. 포인트 충전 또는 사용을 위한 검증을 수행합니다.
    4. 포인트 업데이트 및 포인트 내역을 기록합니다.
    5. 작업이 완료된 후 락을 해제합니다.

    ```java
    // 예시: 포인트 충전 메서드
    @Transactional
    public UserPoint chargeUserPoint(long userId, long amount) {
        Lock lock = getUserLock(userId);
        lock.lock();
        try {
            userValidate.validateUser(userId);
            pointValidate.validateChargeAmount(amount);
            UserPoint updatedUserPoint = userPointTable.insertOrUpdate(userId, amount);
            pointHistoryTable.insert(userId, amount, TransactionType.CHARGE, System.currentTimeMillis());
            return updatedUserPoint;
        } finally {
            lock.unlock();
        }
    }
    ```

### 4. 트랜잭션 관리
- 각 메서드는 `@Transactional` 어노테이션을 사용하여 Spring의 트랜잭션 관리 기능을 활용합니다. 이를 통해 포인트 충전 및 사용 작업이 원자적으로 처리되어 데이터의 일관성을 보장합니다.
- `@Transactional(readOnly = true)`를 사용하여 포인트 조회 메서드의 성능을 최적화합니다.

## 결론
`PointService` 클래스는 사용자 포인트 시스템에서 동시성 문제를 효과적으로 해결하기 위해 `ReentrantLock`과 `ConcurrentHashMap`을 사용합니다. 이로 인해 여러 스레드가 동시에 접근하더라도 데이터의 일관성과 안전성을 유지할 수 있습니다. 트랜잭션 관리를 통해 포인트 충전 및 사용 과정에서 발생할 수 있는 오류를 최소화하고, 전체 시스템의 신뢰성을 향상시킵니다.

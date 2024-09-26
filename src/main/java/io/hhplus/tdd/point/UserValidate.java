package io.hhplus.tdd.point;

import io.hhplus.tdd.ErrorResponse;
import io.hhplus.tdd.database.UserPointTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserValidate {
    private static final Logger log = LoggerFactory.getLogger(UserValidate.class);
    private final UserPointTable userPointTable;

    public UserValidate(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    public void validateUser(long userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자 ID입니다.");
        }

        // 유저가 존재하는지 체크
        if (userPointTable.selectById(userId) == null) {
            throw new IllegalArgumentException("사용자를 찾을 수 없습니다.");
        }
    }
}

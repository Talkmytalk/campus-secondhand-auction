package edu.campus.auction.domain;

import java.math.BigDecimal;

public final class Money {
    private Money() {}

    public static BigDecimal positive(BigDecimal value) {
        if (value == null || value.signum() <= 0 || value.scale() > 2) {
            throw new RuleViolation("INVALID_AMOUNT", "金额必须大于零，且最多保留两位小数");
        }
        return value.setScale(2);
    }
}

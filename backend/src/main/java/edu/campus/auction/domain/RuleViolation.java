package edu.campus.auction.domain;

/** 业务规则不满足时，保留稳定的错误码和可读说明。 */
public class RuleViolation extends RuntimeException {
    private final String code;

    public RuleViolation(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}

package cn.itcast.RPC框架;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CircuitBreaker {
    private enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    private State state = State.CLOSED;
    private final int failureThreshold;
    private final long timeout;
    private final int successThreshold;
    private int failureCount = 0;
    private int successCount = 0;
    private long lastFailureTime;

    public CircuitBreaker(int failureThreshold, long timeout, int successThreshold) {
        this.failureThreshold = failureThreshold;
        this.timeout = timeout;
        this.successThreshold = successThreshold;
    }

    public boolean canExecute() {
        switch (state) {
            case CLOSED:
                return true;
            case OPEN:
                if (System.currentTimeMillis() - lastFailureTime > timeout) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            case HALF_OPEN:
                return true;
            default:
                return true;
        }
    }

    public void recordSuccess() {
        switch (state) {
            case CLOSED:
                failureCount = 0;
                break;
            case OPEN:
                break;
            case HALF_OPEN:
                successCount++;
                if (successCount >= successThreshold) {
                    log.info("熔断器关闭，服务恢复正常");
                    state = State.CLOSED;
                    successCount = 0;
                    failureCount = 0;
                }
                break;
        }
    }

    public void recordFailure() {
        failureCount++;
        lastFailureTime = System.currentTimeMillis();

        if (state == State.CLOSED && failureCount >= failureThreshold) {
            log.warn("熔断器打开，服务不可用");
            state = State.OPEN;
        } else if (state == State.HALF_OPEN) {
            log.warn("熔断器重新打开，服务仍然不可用");
            state = State.OPEN;
        }
    }
}

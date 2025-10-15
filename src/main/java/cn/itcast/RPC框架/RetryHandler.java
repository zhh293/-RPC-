package cn.itcast.RPC框架;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;

@Slf4j
public class RetryHandler {
    private final int maxRetries;
    private final long retryInterval;

    public RetryHandler(int maxRetries, long retryInterval) {
        this.maxRetries = maxRetries;
        this.retryInterval = retryInterval;
    }

    public Object executeWithRetry(Callable<Object> callable) throws Exception {
        Exception lastException = null;

        for (int i = 0; i <= maxRetries; i++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries) {
                    log.warn("调用失败，第{}次重试，错误: {}", i + 1, e.getMessage());
                    try {
                        Thread.sleep(retryInterval);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                }
            }
        }

        log.error("重试{}次后仍然失败", maxRetries);
        throw lastException;
    }
}

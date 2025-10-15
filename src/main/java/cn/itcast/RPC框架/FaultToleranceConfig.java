package cn.itcast.RPC框架;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FaultToleranceConfig {
    private boolean circuitBreakerEnabled = true;
    private int failureThreshold = 5;
    private long circuitBreakerTimeout = 10000;
    private int successThreshold = 3;
    private boolean retryEnabled = true;
    private int maxRetries = 3;
    private long retryInterval = 1000;
}

package cn.itcast.RPC框架;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceCommon {
    private String serviceName;
    private String serviceAddress;
    private String servicePort;
    private String serviceClass;
}

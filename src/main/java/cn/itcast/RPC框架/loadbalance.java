package cn.itcast.RPC框架;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 负载均衡器类
 * 提供多种负载均衡算法来分发客户端请求到不同的服务器
 */
public class loadbalance {
    // 服务器列表
    private static List<Server> serverList = new ArrayList<>();
    // 当前轮询索引（用于轮询算法）
    private AtomicInteger currentIndex = new AtomicInteger(0);
    // 随机数生成器
    private Random random = new Random();
    // 服务器调用统计（用于最少连接算法）
    private ConcurrentHashMap<Server, AtomicInteger> serverCallCount = new ConcurrentHashMap<>();

    // 服务器内部类
    public static class Server {
        private String address;
        private int weight;
        private boolean active;

        public Server(String address, int weight) {
            this.address = address;
            this.weight = weight;
            this.active = true;
        }

        // getters and setters
        public String getAddress() { return address; }
        public int getWeight() { return weight; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    static {
        // 添加带权重的服务器示例
        serverList.add(new Server("127.0.0.1:8080", 2));
        serverList.add(new Server("127.0.0.1:8081", 3));
        serverList.add(new Server("127.0.0.1:8082", 1));
        serverList.add(new Server("127.0.0.1:8083", 4));
        serverList.add(new Server("127.0.0.1:8084", 2));
    }

    /**
     * 加权轮询算法 (Weighted Round Robin)
     * 根据服务器权重分配请求
     * @return 选中的服务器
     */
    public Server selectServerWeightedRoundRobin() {
        if (serverList.isEmpty()) {
            return null;
        }

        int totalWeight = serverList.stream()
                .filter(Server::isActive)
                .mapToInt(Server::getWeight)
                .sum();

        if (totalWeight <= 0) {
            return null;
        }

        int currentPos = random.nextInt(totalWeight);
        for (Server server : serverList) {
            if (!server.isActive()) continue;
            currentPos -= server.getWeight();
            if (currentPos < 0) {
                return server;
            }
        }
        return serverList.get(0);
    }

    /**
     * 最少连接数算法 (Least Connections)
     * 选择当前连接数最少的活跃服务器
     * @return 选中的服务器
     */
    public Server selectServerLeastConnections() {
        if (serverList.isEmpty()) {
            return null;
        }

        Server selectedServer = null;
        int minConnections = Integer.MAX_VALUE;

        for (Server server : serverList) {
            if (!server.isActive()) continue;

            AtomicInteger count = serverCallCount.get(server);
            int connections = (count == null) ? 0 : count.get();

            if (connections < minConnections) {
                minConnections = connections;
                selectedServer = server;
            }
        }

        // 增加选中服务器的连接计数
        if (selectedServer != null) {
            serverCallCount.computeIfAbsent(selectedServer, s -> new AtomicInteger(0))
                    .incrementAndGet();
        }

        return selectedServer;
    }

    /**
     * 源地址哈希算法 (IP Hash)
     * 根据客户端IP地址进行哈希计算，保证同一IP总是路由到同一服务器
     * @param clientIp 客户端IP地址
     * @return 选中的服务器
     */
    public Server selectServerIpHash(String clientIp) {
        if (serverList.isEmpty() || clientIp == null || clientIp.isEmpty()) {
            return null;
        }

        List<Server> activeServers = new ArrayList<>();
        for (Server server : serverList) {
            if (server.isActive()) {
                activeServers.add(server);
            }
        }

        if (activeServers.isEmpty()) {
            return null;
        }

        int hash = clientIp.hashCode();
        int index = Math.abs(hash) % activeServers.size();
        return activeServers.get(index);
    }

    /**
     * 响应时间加权算法 (Response Time Weighted)
     * 根据服务器响应时间动态调整权重
     * @param responseTimes 服务器响应时间映射
     * @return 选中的服务器
     */
    public Server selectServerResponseTimeWeighted(ConcurrentHashMap<Server, Long> responseTimes) {
        if (serverList.isEmpty()) {
            return null;
        }

        long maxResponseTime = responseTimes.values().stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(1000L); // 默认最大响应时间1000ms

        long totalWeight = 0;
        for (Server server : serverList) {
            if (!server.isActive()) continue;
            long responseTime = responseTimes.getOrDefault(server, maxResponseTime);
            // 响应时间越短，权重越高
            long weight = Math.max(1, maxResponseTime - responseTime + 1);
            totalWeight += weight;
        }

        if (totalWeight <= 0) {
            return getRandomActiveServer();
        }

        long randomValue = random.nextLong() % totalWeight;
        long currentWeight = 0;

        for (Server server : serverList) {
            if (!server.isActive()) continue;
            long responseTime = responseTimes.getOrDefault(server, maxResponseTime);
            long weight = Math.max(1, maxResponseTime - responseTime + 1);
            currentWeight += weight;
            if (randomValue <= currentWeight) {
                return server;
            }
        }

        return getRandomActiveServer();
    }

    /**
     * 获取随机活跃服务器
     * @return 随机选中的活跃服务器
     */
    private Server getRandomActiveServer() {
        List<Server> activeServers = new ArrayList<>();
        for (Server server : serverList) {
            if (server.isActive()) {
                activeServers.add(server);
            }
        }
        if (activeServers.isEmpty()) {
            return null;
        }
        return activeServers.get(random.nextInt(activeServers.size()));
    }

    // 保留原有的方法...
    public void addServer(Object server) {
        if (server instanceof Server) {
            serverList.add((Server) server);
        }
    }

    public void removeServer(Object server) {
        serverList.remove(server);
    }

    public List<Server> getServerList() {
        return new ArrayList<>(serverList);
    }

    public Server selectServerRoundRobin() {
        if (serverList.isEmpty()) {
            return null;
        }
        List<Server> activeServers = new ArrayList<>();
        for (Server server : serverList) {
            if (server.isActive()) {
                activeServers.add(server);
            }
        }
        if (activeServers.isEmpty()) {
            return null;
        }
        int index = currentIndex.getAndIncrement() % activeServers.size();
        if (currentIndex.get() >= activeServers.size()) {
            currentIndex.set(0);
        }
        return activeServers.get(index);
    }

    public Server selectServerRandom() {
        return getRandomActiveServer();
    }

    public int getServerCount() {
        return serverList.size();
    }
}

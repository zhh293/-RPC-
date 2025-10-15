package cn.itcast.config;

import cn.itcast.protocol.Serializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public abstract class Config {
    static Properties properties;
    static {
        try (InputStream in = Config.class.getResourceAsStream("/application.properties")) {
            properties = new Properties();
            properties.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    public static int getServerPort() {
        String value = properties.getProperty("server.port");
        if(value == null) {
            return 8080;
        } else {
            return Integer.parseInt(value);
        }
    }
    public static Serializer.Algorithm getSerializerAlgorithm() {
        String value = properties.getProperty("serializer.algorithm");
        if(value == null) {
            return Serializer.Algorithm.Java;
        } else {
            return Serializer.Algorithm.valueOf(value);
        }
    }
    //读取配置文件中的服务相关信息，首先读取服务的名称，返回一个集合
    public static String[] getServiceNames() {
        return properties.getProperty("services").split(",");
    }
    //服务的类所在文件地址，返回一个集合
    public static String[] getServiceClasses() {
        return properties.getProperty("service.classes").split(",");
    }
    //服务的地址，返回一个集合
    public static String[] getServiceAddresses() {
        return properties.getProperty("service.addresses").split(",");
    }
    //服务的端口号
    public static int[] getServicePorts() {
        String[] ports = properties.getProperty("service.ports").split(",");
        int[] portsInt = new int[ports.length];
        for(int i = 0; i < ports.length; i++) {
            portsInt[i] = Integer.parseInt(ports[i]);
        }
        return portsInt;
    }

    public static String getRegisterCenterHost() {
        return properties.getProperty("register.center.host");
    }
    public static int getRegisterCenterPort() {
        String value = properties.getProperty("register.center.port");
        if(value == null) {
            return 8081;
        } else {
            return Integer.parseInt(value);
        }
    }

}
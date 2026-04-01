package cn.itcast.模拟框架接受前端请求并返回.试试校园网;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NettyClient {

    private static final String USERNAME = "20242081353";
    private static final String PASSWORD = "Zhang20061023@";

    // 固定配置
    private static final String HOST = "sso.dlut.edu.cn";
    private static final int PORT = 443;
    private static final String SERVICE = "https://portal.dlut.edu.cn/tp";
    private static final String LOGIN_PAGE_URI = "/cas/login?service="
            + URLEncoder.encode(SERVICE, StandardCharsets.UTF_8);
    private static final String TOKEN_COOKIE_NAME = "datalook_reimbursement_token";

    private static String COOKIE_STR = "";
    private static String LT_TOKEN = "";
    private static String EXECUTION = "";
    private static String POST_URI = "/cas/login";
    private static String EXTRACTED_TOKEN = "";
    private static final Map<String, String> COOKIE_JAR = new LinkedHashMap<>();
    private static final AtomicBoolean LOGIN_SUBMITTED = new AtomicBoolean(false);
    private static final AtomicInteger RESPONSE_STAGE = new AtomicInteger(0);

    public static void main(String[] args) throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup();

        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), HOST, PORT));
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
                                    updateCookies(response.headers().getAll(HttpHeaderNames.SET_COOKIE));

                                    int stage = RESPONSE_STAGE.get();
                                    if (stage == 0) {
                                        handleLoginPage(ctx, response);
                                        RESPONSE_STAGE.set(1);
                                        return;
                                    }

                                    if (stage == 1) {
                                        handleLoginSubmitResponse(ctx, response);
                                        RESPONSE_STAGE.set(2);
                                        return;
                                    }

                                    System.out.println("\n📌 最终响应码: " + response.status());
                                    System.out.println("最终返回长度: " + response.content().readableBytes());
                                    ctx.channel().close();
                                }
                            });
                        }
                    });

            Channel channel = b.connect(HOST, PORT).sync().channel();

            FullHttpRequest getReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, LOGIN_PAGE_URI);
            getReq.headers()
                    .set(HttpHeaderNames.HOST, HOST)
                    .set(HttpHeaderNames.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
                    .set(HttpHeaderNames.ACCEPT,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .set(HttpHeaderNames.CONNECTION, "keep-alive")
                    .set("upgrade-insecure-requests", "1");

            channel.writeAndFlush(getReq).sync();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void handleLoginPage(ChannelHandlerContext ctx, FullHttpResponse response) {
        System.out.println("登录页响应码: " + response.status());
        String html = response.content().toString(StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(html);

        LT_TOKEN = valueOfInput(doc, "lt");
        EXECUTION = valueOfInput(doc, "execution");
        String formAction = resolveFormAction(doc);
        POST_URI = ensureServiceParam(formAction);

        COOKIE_JAR.putIfAbsent("cas_hash", "");

        refreshCookieString();
        System.out.println("完整Cookie: " + COOKIE_STR);
        System.out.println("登录令牌LT: " + LT_TOKEN);
        System.out.println("execution: " + EXECUTION);
        System.out.println("POST地址: " + POST_URI);

        try {
            submitLogin(ctx);
            LOGIN_SUBMITTED.set(true);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.channel().close();
        }
    }

    private static void handleLoginSubmitResponse(ChannelHandlerContext ctx, FullHttpResponse response) {
        if (!LOGIN_SUBMITTED.get()) {
            ctx.channel().close();
            return;
        }
        System.out.println("\n📌 登录响应码: " + response.status());
        String location = response.headers().get(HttpHeaderNames.LOCATION);
        List<String> setCookies = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
        if (response.status().equals(HttpResponseStatus.FOUND)) {
            System.out.println("✅ 登录成功，服务器返回302");
            System.out.println("重定向Location: " + location);
            System.out.println("Set-Cookie: " + setCookies);
            if (location != null && !location.isBlank()) {
                tryFollowRedirect(ctx, location);
                return;
            }
        } else {
            String body = response.content().toString(StandardCharsets.UTF_8);
            System.out.println("❌ 登录失败，响应体片段: " + body.substring(0, Math.min(body.length(), 300)));
        }
        ctx.channel().close();
    }

    private static void tryFollowRedirect(ChannelHandlerContext ctx, String location) {
        try {
            URI uri = URI.create(location);
            String host = uri.getHost();
            if (host == null || !HOST.equalsIgnoreCase(host)) {
                System.out.println("ℹ️ 检测到跨域重定向，开始新建连接继续访问: " + location);
                followCrossDomainRedirect(location);
                ctx.channel().close();
                return;
            }
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            String targetUri = (path == null || path.isBlank()) ? "/" : path;
            if (query != null && !query.isBlank()) {
                targetUri = targetUri + "?" + query;
            }
            FullHttpRequest redirectReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, targetUri);
            redirectReq.headers()
                    .set(HttpHeaderNames.HOST, HOST)
                    .set(HttpHeaderNames.COOKIE, COOKIE_STR)
                    .set(HttpHeaderNames.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
                    .set(HttpHeaderNames.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .set(HttpHeaderNames.CONNECTION, "close");
            ctx.channel().writeAndFlush(redirectReq);
        } catch (Exception e) {
            e.printStackTrace();
            ctx.channel().close();
        }
    }

    private static void followCrossDomainRedirect(String location) {
        String current = location;
        Map<String, String> portalCookieJar = new LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            PortalResponse result = fetchPortalOnce(current, portalCookieJar);
            if (result == null) {
                return;
            }
            updateCookieJar(portalCookieJar, result.setCookies);
            System.out.println("\n🌐 门户响应码: " + result.status);
            if (result.location != null && !result.location.isBlank()) {
                System.out.println("门户重定向: " + result.location);
            }
            System.out.println("门户响应体片段: " + result.bodySnippet);
            if (result.status.code() >= 300 && result.status.code() < 400 && result.location != null
                    && !result.location.isBlank()) {
                current = URI.create(current).resolve(result.location).toString();
                continue;
            }
            extractTokenFromBody(result.fullBody);
            fetchEcardToken(portalCookieJar);
            return;
        }
        fetchEcardToken(portalCookieJar);
    }

    private static PortalResponse fetchPortalOnce(String url, Map<String, String> cookieJar) {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase();
            boolean https = "https".equals(scheme);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            int port = uri.getPort();
            if (port == -1) {
                port = https ? 443 : 80;
            }
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            String query = uri.getRawQuery();
            String target = query == null || query.isBlank() ? path : path + "?" + query;

            String[] locationHolder = new String[1];
            String[] bodyHolder = new String[1];
            HttpResponseStatus[] statusHolder = new HttpResponseStatus[1];
            List<String>[] setCookieHolder = new List[] { new ArrayList<>() };
            CountDownLatch latch = new CountDownLatch(1);

            SslContext sslContext = null;
            if (https) {
                sslContext = SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build();
            }
            SslContext finalSslContext = sslContext;
            int finalPort = port;

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            if (finalSslContext != null) {
                                ch.pipeline().addLast(finalSslContext.newHandler(ch.alloc(), host, finalPort));
                            }
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response) {
                                    statusHolder[0] = response.status();
                                    locationHolder[0] = response.headers().get(HttpHeaderNames.LOCATION);
                                    setCookieHolder[0] = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
                                    String body = response.content().toString(StandardCharsets.UTF_8);
                                    bodyHolder[0] = body;
                                    latch.countDown();
                                    ctx.channel().close();
                                }
                            });
                        }
                    });

            Channel channel = bootstrap.connect(host, finalPort).sync().channel();
            FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, target);
            req.headers()
                    .set(HttpHeaderNames.HOST, finalPort == 80 || finalPort == 443 ? host : host + ":" + finalPort)
                    .set(HttpHeaderNames.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
                    .set(HttpHeaderNames.ACCEPT,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
                    .set(HttpHeaderNames.CONNECTION, "close");
            String cookieHeader = buildCookieHeader(cookieJar);
            if (!cookieHeader.isBlank()) {
                req.headers().set(HttpHeaderNames.COOKIE, cookieHeader);
            }
            channel.writeAndFlush(req).sync();
            latch.await(10, TimeUnit.SECONDS);
            channel.closeFuture().sync();
            if (statusHolder[0] == null) {
                return null;
            }
            String fullBody = bodyHolder[0] == null ? "" : bodyHolder[0];
            return new PortalResponse(statusHolder[0], locationHolder[0], setCookieHolder[0], fullBody,
                    fullBody.substring(0, Math.min(fullBody.length(), 300)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void updateCookieJar(Map<String, String> jar, List<String> setCookies) {
        if (setCookies == null) {
            return;
        }
        for (String c : setCookies) {
            String kv = c.split(";", 2)[0].trim();
            int idx = kv.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String name = kv.substring(0, idx);
            String value = kv.substring(idx + 1);
            jar.put(name, value);
            if (TOKEN_COOKIE_NAME.equalsIgnoreCase(name) && value != null && !value.isBlank()) {
                EXTRACTED_TOKEN = value;
                System.out.println("✅ 提取到Token: " + EXTRACTED_TOKEN);
            }
        }
    }

    private static void fetchEcardToken(Map<String, String> cookieJar) {
        if (EXTRACTED_TOKEN != null && !EXTRACTED_TOKEN.isBlank()) {
            return;
        }
        String current = "http://ecardpayment.dlut.edu.cn/";
        for (int i = 0; i < 8; i++) {
            PortalResponse response = fetchPortalOnce(current, cookieJar);
            if (response == null) {
                break;
            }
            updateCookieJar(cookieJar, response.setCookies);
            extractTokenFromBody(response.fullBody);
            if (EXTRACTED_TOKEN != null && !EXTRACTED_TOKEN.isBlank()) {
                return;
            }
            if (response.status.code() >= 300 && response.status.code() < 400 && response.location != null
                    && !response.location.isBlank()) {
                current = URI.create(current).resolve(response.location).toString();
                continue;
            }
            break;
        }
        if (EXTRACTED_TOKEN == null || EXTRACTED_TOKEN.isBlank()) {
            System.out.println("⚠️ 暂未提取到Token");
        }
    }

    private static void extractTokenFromBody(String body) {
        if (body == null || body.isBlank() || (EXTRACTED_TOKEN != null && !EXTRACTED_TOKEN.isBlank())) {
            return;
        }
        Pattern p = Pattern.compile("datalook_reimbursement_token[\"'=:\\s]+([A-Za-z0-9\\-_.]+)");
        Matcher m = p.matcher(body);
        if (m.find()) {
            EXTRACTED_TOKEN = m.group(1);
            System.out.println("✅ 提取到Token: " + EXTRACTED_TOKEN);
        }
    }

    private static String buildCookieHeader(Map<String, String> jar) {
        StringJoiner joiner = new StringJoiner("; ");
        for (Map.Entry<String, String> entry : jar.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        return joiner.toString();
    }

    private static String valueOfInput(Document doc, String name) {
        Element input = doc.selectFirst("input[name=" + name + "]");
        if (input == null) {
            return "";
        }
        return input.val().trim();
    }

    private static String resolveFormAction(Document doc) {
        Element form = doc.selectFirst("form");
        if (form == null) {
            return "/cas/login";
        }
        String action = form.attr("action").trim();
        if (action.isBlank()) {
            return "/cas/login";
        }
        if (action.startsWith("http://") || action.startsWith("https://")) {
            URI uri = URI.create(action);
            String path = uri.getRawPath();
            String query = uri.getRawQuery();
            String result = (path == null || path.isBlank()) ? "/" : path;
            if (query != null && !query.isBlank()) {
                result = result + "?" + query;
            }
            return result;
        }
        if (!action.startsWith("/")) {
            return "/" + action;
        }
        return action;
    }

    private static String ensureServiceParam(String uri) {
        URI u = URI.create(uri);
        String path = u.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/cas/login";
        }
        String query = u.getRawQuery();
        String serviceParam = "service=" + URLEncoder.encode(SERVICE, StandardCharsets.UTF_8);
        if (query == null || query.isBlank()) {
            return path + "?" + serviceParam;
        }
        if (query.contains("service=")) {
            return path + "?" + query;
        }
        return path + "?" + query + "&" + serviceParam;
    }

    private static void updateCookies(List<String> cookies) {
        for (String c : cookies) {
            String kv = c.split(";", 2)[0].trim();
            int idx = kv.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String name = kv.substring(0, idx);
            String value = kv.substring(idx + 1);
            COOKIE_JAR.put(name, value);
        }
        refreshCookieString();
    }

    private static void refreshCookieString() {
        StringJoiner joiner = new StringJoiner("; ");
        for (Map.Entry<String, String> entry : COOKIE_JAR.entrySet()) {
            joiner.add(entry.getKey() + "=" + entry.getValue());
        }
        COOKIE_STR = joiner.toString();
    }

    private static void submitLogin(ChannelHandlerContext ctx) throws Exception {
        ScriptEngine js = new NashornScriptEngineFactory().getScriptEngine();
        js.eval(JavaScriptDes.javascriptCode);
        String rsa = (String) js.eval("strEnc('" + USERNAME + PASSWORD + LT_TOKEN + "','1','2','3')");
        System.out.println("加密结果rsa: " + rsa);

        String form = String.format(
                "rsa=%s&ul=%d&pl=%d&sl=0&lt=%s&execution=%s&_eventId=submit",
                URLEncoder.encode(rsa, StandardCharsets.UTF_8),
                USERNAME.length(),
                PASSWORD.length(),
                URLEncoder.encode(LT_TOKEN, StandardCharsets.UTF_8),
                URLEncoder.encode(EXECUTION, StandardCharsets.UTF_8));
        byte[] formBytes = form.getBytes(StandardCharsets.UTF_8);

        FullHttpRequest postReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, POST_URI);
        postReq.headers()
                .set(HttpHeaderNames.HOST, HOST)
                .set(HttpHeaderNames.ORIGIN, "https://sso.dlut.edu.cn")
                .set(HttpHeaderNames.REFERER, "https://sso.dlut.edu.cn/login")
                .set(HttpHeaderNames.COOKIE, COOKIE_STR)
                .set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .set(HttpHeaderNames.CONTENT_LENGTH, formBytes.length)
                .set(HttpHeaderNames.ACCEPT,
                        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .set(HttpHeaderNames.ACCEPT_LANGUAGE, "zh-CN,zh;q=0.9,en;q=0.8")
                .set(HttpHeaderNames.CACHE_CONTROL, "max-age=0")
                .set("upgrade-insecure-requests", "1")
                .set(HttpHeaderNames.USER_AGENT,
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0")
                .set(HttpHeaderNames.CONNECTION, "keep-alive");

        postReq.content().writeBytes(formBytes);
        ctx.channel().writeAndFlush(postReq);
    }

    // 加密工具类不动
    public static String strEnc(String data, String k1, String k2, String k3) throws Exception {
        byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
        byte[] encrypted = des3Encrypt(bytes, k1.getBytes(), k2.getBytes(), k3.getBytes());
        return byteArrayToHexString(encrypted).toUpperCase();
    }

    public static byte[] des3Encrypt(byte[] data, byte[] k1, byte[] k2, byte[] k3) throws Exception {
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("DES/ECB/PKCS5Padding");
        javax.crypto.SecretKeyFactory keyFactory = javax.crypto.SecretKeyFactory.getInstance("DES");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyFactory.generateSecret(new javax.crypto.spec.DESKeySpec(k1)));
        byte[] b1 = cipher.doFinal(data);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyFactory.generateSecret(new javax.crypto.spec.DESKeySpec(k2)));
        byte[] b2 = cipher.doFinal(b1);
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keyFactory.generateSecret(new javax.crypto.spec.DESKeySpec(k3)));
        return cipher.doFinal(b2);
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() == 1)
                sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    private static final class PortalResponse {
        private final HttpResponseStatus status;
        private final String location;
        private final List<String> setCookies;
        private final String fullBody;
        private final String bodySnippet;

        private PortalResponse(HttpResponseStatus status, String location, List<String> setCookies, String fullBody,
                String bodySnippet) {
            this.status = status;
            this.location = location;
            this.setCookies = setCookies;
            this.fullBody = fullBody;
            this.bodySnippet = bodySnippet;
        }
    }
}

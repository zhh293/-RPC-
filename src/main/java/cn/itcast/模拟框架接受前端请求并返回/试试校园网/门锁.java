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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringJoiner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class 门锁 {

    private static final String USERNAME = "20242081353";
    private static final String PASSWORD = "Zhang20061023@";
    private static final String SSO_HOST = "sso.dlut.edu.cn";
    private static final int SSO_PORT = 443;
    private static final String MENJIN_SERVICE = "http://menjin.dlut.edu.cn/cser/static/menjin/index.html";
    private static final String LOGIN_PAGE_URI = "/cas/login?service="
            + URLEncoder.encode(MENJIN_SERVICE, StandardCharsets.UTF_8);

    private static final String OPEN_API = "http://menjin.dlut.edu.cn/cser/device/info/command/sendRoomBatch";
    private static final String PERSON_ID = USERNAME;
    private static final String DEVICE_CODE = "DL-YX-106118";
    private static final String PROJECT_CD = "DA_LIAN_LI_GONG_MENJIN";
    private static final String AUTH_SECRET = "2323dsfadfewrasa3434";

    private static final Map<String, String> COOKIE_JAR = new LinkedHashMap<>();
    private static String LT_TOKEN = "";
    private static String EXECUTION = "";
    private static String POST_URI = "/cas/login";
    private static String MENJIN_TOKEN = "";

    public static void main(String[] args) throws Exception {
        doCasLogin();
        if (MENJIN_TOKEN == null || MENJIN_TOKEN.isBlank()) {
            System.out.println("⚠️ 未提取到门禁token，先只输出cookie");
            System.out.println("当前Cookie: " + buildCookieHeader(COOKIE_JAR));
            return;
        }
        System.out.println("✅ 门禁Token: " + MENJIN_TOKEN);
        ResponseData openResp = callOpenDoorApi();
        if (openResp != null) {
            System.out.println("📌 开门接口响应码: " + openResp.status);
            System.out.println("📦 开门接口返回: " + openResp.bodySnippet);
        }
    }

    private static void doCasLogin() throws Exception {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<NioSocketChannel>() {
                        @Override
                        protected void initChannel(NioSocketChannel ch) {
                            ch.pipeline().addLast(sslContext.newHandler(ch.alloc(), SSO_HOST, SSO_PORT));
                            ch.pipeline().addLast(new HttpClientCodec());
                            ch.pipeline().addLast(new HttpObjectAggregator(1024 * 1024));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<FullHttpResponse>() {
                                private int stage = 0;

                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse response)
                                        throws Exception {
                                    updateCookieJar(COOKIE_JAR, response.headers().getAll(HttpHeaderNames.SET_COOKIE));
                                    if (stage == 0) {
                                        stage = 1;
                                        handleLoginPage(ctx, response);
                                        return;
                                    }
                                    if (stage == 1) {
                                        stage = 2;
                                        handleLoginSubmit(ctx, response);
                                        return;
                                    }
                                    ctx.channel().close();
                                }
                            });
                        }
                    });

            Channel channel = b.connect(SSO_HOST, SSO_PORT).sync().channel();
            FullHttpRequest getReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, LOGIN_PAGE_URI);
            getReq.headers()
                    .set(HttpHeaderNames.HOST, SSO_HOST)
                    .set(HttpHeaderNames.USER_AGENT, ua())
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

    private static void handleLoginPage(ChannelHandlerContext ctx, FullHttpResponse response) throws Exception {
        String html = response.content().toString(StandardCharsets.UTF_8);
        Document doc = Jsoup.parse(html);
        LT_TOKEN = valueOfInput(doc, "lt");
        EXECUTION = valueOfInput(doc, "execution");
        POST_URI = ensureServiceParam(resolveFormAction(doc));
        String rsa = encryptRsa(USERNAME + PASSWORD + LT_TOKEN);
        String form = "rsa=" + urlEncode(rsa)
                + "&service=" + urlEncode(MENJIN_SERVICE)
                + "&ul=" + USERNAME.length()
                + "&pl=" + PASSWORD.length()
                + "&sl=0"
                + "&lt=" + urlEncode(LT_TOKEN)
                + "&execution=" + urlEncode(EXECUTION)
                + "&_eventId=submit";
        byte[] formBytes = form.getBytes(StandardCharsets.UTF_8);
        FullHttpRequest postReq = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, POST_URI);
        postReq.headers()
                .set(HttpHeaderNames.HOST, SSO_HOST)
                .set(HttpHeaderNames.ORIGIN, "https://sso.dlut.edu.cn")
                .set(HttpHeaderNames.REFERER, "https://sso.dlut.edu.cn" + LOGIN_PAGE_URI)
                .set(HttpHeaderNames.COOKIE, buildCookieHeader(COOKIE_JAR))
                .set(HttpHeaderNames.CONTENT_TYPE, "application/x-www-form-urlencoded")
                .set(HttpHeaderNames.CONTENT_LENGTH, formBytes.length)
                .set(HttpHeaderNames.USER_AGENT, ua())
                .set(HttpHeaderNames.CONNECTION, "close");
        postReq.content().writeBytes(formBytes);
        ctx.channel().writeAndFlush(postReq);
    }

    private static void handleLoginSubmit(ChannelHandlerContext ctx, FullHttpResponse response) {
        System.out.println("📌 CAS登录响应: " + response.status());
        String location = response.headers().get(HttpHeaderNames.LOCATION);
        if (response.status().equals(HttpResponseStatus.FOUND) && location != null && !location.isBlank()) {
            System.out.println("✅ CAS重定向: " + location);
            followRedirectChain(location);
        } else {
            String body = response.content().toString(StandardCharsets.UTF_8);
            System.out.println("❌ CAS登录失败片段: " + body.substring(0, Math.min(300, body.length())));
        }
        ctx.channel().close();
    }

    private static void followRedirectChain(String startUrl) {
        String current = startUrl;
        for (int i = 0; i < 10; i++) {
            ResponseData r = fetchOnce(current, COOKIE_JAR);
            if (r == null) {
                break;
            }
            updateCookieJar(COOKIE_JAR, r.setCookies);
            extractToken(r.body);
            System.out.println("🌐 跳转响应: " + r.status + " -> " + current);
            if (r.location != null && !r.location.isBlank() && r.status.code() >= 300 && r.status.code() < 400) {
                current = URI.create(current).resolve(r.location).toString();
                continue;
            }
            break;
        }
    }

    private static ResponseData callOpenDoorApi() {
        String body = "commandCode=OPEN"
                + "&commandParams=" + urlEncode("{}")
                + "&conditions=" + urlEncode("{\"personId\":\"" + PERSON_ID + "\",\"delStatus\":\"0\"}")
                + "&deviceCode=" + urlEncode(DEVICE_CODE)
                + "&isCommon=yes"
                + "&pageSize=-1"
                + "&projectCd=" + urlEncode(PROJECT_CD)
                + "&token=" + urlEncode(MENJIN_TOKEN);
        String authTimestamp = String.valueOf(System.currentTimeMillis());
        String nonce = randomAlphaNum(4);
        String authSign = md5Hex(authTimestamp + "#" + AUTH_SECRET + "#" + nonce) + nonce;
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(HttpHeaderNames.ORIGIN.toString(), "http://menjin.dlut.edu.cn");
        headers.put(HttpHeaderNames.REFERER.toString(), "http://menjin.dlut.edu.cn/cser/static/menjin/mine.html");
        headers.put(HttpHeaderNames.CONTENT_TYPE.toString(), "application/x-www-form-urlencoded");
        headers.put("token", MENJIN_TOKEN);
        headers.put("x-auth-token", MENJIN_TOKEN);
        headers.put("Access-Token", MENJIN_TOKEN);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("AUTH-TIMESTAMP", authTimestamp);
        headers.put("AUTH-SIGN", authSign);
        headers.put("auth-timestamp", authTimestamp);
        headers.put("auth-sign", authSign);
        return postOnce(OPEN_API, COOKIE_JAR, headers, body);
    }

    private static ResponseData postOnce(String url, Map<String, String> cookieJar, Map<String, String> extraHeaders,
            String body) {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            URI uri = URI.create(url);
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            int port = uri.getPort() == -1 ? (https ? 443 : 80) : uri.getPort();
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                path += "?" + uri.getRawQuery();
            }
            SslContext sslContext = https
                    ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                    : null;
            String[] location = new String[1];
            String[] bodyArr = new String[1];
            HttpResponseStatus[] statusArr = new HttpResponseStatus[1];
            List<String>[] setCookies = new List[] { new ArrayList<>() };
            CountDownLatch latch = new CountDownLatch(1);

            SslContext finalSslContext = sslContext;
            int finalPort = port;
            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
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
                                    statusArr[0] = response.status();
                                    location[0] = response.headers().get(HttpHeaderNames.LOCATION);
                                    setCookies[0] = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
                                    bodyArr[0] = response.content().toString(StandardCharsets.UTF_8);
                                    ctx.channel().close();
                                    latch.countDown();
                                }
                            });
                        }
                    });

            Channel channel = b.connect(host, port).sync().channel();
            byte[] bodyBytes = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
            FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path);
            req.headers()
                    .set(HttpHeaderNames.HOST, port == 80 || port == 443 ? host : host + ":" + port)
                    .set(HttpHeaderNames.USER_AGENT, ua())
                    .set(HttpHeaderNames.ACCEPT, "application/json, text/plain, */*")
                    .set(HttpHeaderNames.CONTENT_LENGTH, bodyBytes.length)
                    .set(HttpHeaderNames.CONNECTION, "keep-alive");
            String cookieHeader = buildCookieHeader(cookieJar);
            if (!cookieHeader.isBlank()) {
                req.headers().set(HttpHeaderNames.COOKIE, cookieHeader);
            }
            for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
                req.headers().set(entry.getKey(), entry.getValue());
            }
            req.content().writeBytes(bodyBytes);
            channel.writeAndFlush(req).sync();
            latch.await(10, TimeUnit.SECONDS);
            channel.closeFuture().sync();
            if (statusArr[0] == null) {
                return null;
            }
            return new ResponseData(statusArr[0], location[0], setCookies[0], bodyArr[0] == null ? "" : bodyArr[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            group.shutdownGracefully();
        }
    }

    private static ResponseData fetchOnce(String url, Map<String, String> cookieJar) {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        try {
            URI uri = URI.create(url);
            boolean https = "https".equalsIgnoreCase(uri.getScheme());
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return null;
            }
            int port = uri.getPort() == -1 ? (https ? 443 : 80) : uri.getPort();
            String path = uri.getRawPath();
            if (path == null || path.isBlank()) {
                path = "/";
            }
            if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
                path += "?" + uri.getRawQuery();
            }
            SslContext sslContext = https
                    ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                    : null;
            String[] location = new String[1];
            String[] body = new String[1];
            HttpResponseStatus[] status = new HttpResponseStatus[1];
            List<String>[] setCookies = new List[] { new ArrayList<>() };
            CountDownLatch latch = new CountDownLatch(1);
            SslContext finalSslContext = sslContext;
            int finalPort = port;

            Bootstrap b = new Bootstrap();
            b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
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
                                    status[0] = response.status();
                                    location[0] = response.headers().get(HttpHeaderNames.LOCATION);
                                    setCookies[0] = response.headers().getAll(HttpHeaderNames.SET_COOKIE);
                                    body[0] = response.content().toString(StandardCharsets.UTF_8);
                                    ctx.channel().close();
                                    latch.countDown();
                                }
                            });
                        }
                    });

            Channel channel = b.connect(host, port).sync().channel();
            FullHttpRequest req = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, path);
            req.headers()
                    .set(HttpHeaderNames.HOST, port == 80 || port == 443 ? host : host + ":" + port)
                    .set(HttpHeaderNames.USER_AGENT, ua())
                    .set(HttpHeaderNames.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .set(HttpHeaderNames.CONNECTION, "keep-alive");
            String cookieHeader = buildCookieHeader(cookieJar);
            if (!cookieHeader.isBlank()) {
                req.headers().set(HttpHeaderNames.COOKIE, cookieHeader);
            }
            channel.writeAndFlush(req).sync();
            latch.await(10, TimeUnit.SECONDS);
            channel.closeFuture().sync();
            if (status[0] == null) {
                return null;
            }
            return new ResponseData(status[0], location[0], setCookies[0], body[0] == null ? "" : body[0]);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            group.shutdownGracefully();
        }
    }

    private static void extractToken(String body) {
        if (MENJIN_TOKEN != null && !MENJIN_TOKEN.isBlank()) {
            return;
        }
        String cookieToken = COOKIE_JAR.get("shfb-token");
        if (cookieToken != null && !cookieToken.isBlank()) {
            MENJIN_TOKEN = cookieToken;
            return;
        }
        if (body == null || body.isBlank()) {
            return;
        }
        Pattern p = Pattern.compile("(shfb-token|token|accessToken)[\"'=:\\s]+([A-Za-z0-9\\-_.]+)");
        Matcher m = p.matcher(body);
        if (m.find()) {
            MENJIN_TOKEN = m.group(2);
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
            jar.put(kv.substring(0, idx), kv.substring(idx + 1));
        }
        String token = jar.get("shfb-token");
        if (token != null && !token.isBlank()) {
            MENJIN_TOKEN = token;
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
        return input == null ? "" : input.val().trim();
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
        if (!action.startsWith("http://") && !action.startsWith("https://")) {
            return action.startsWith("/") ? action : "/" + action;
        }
        URI uri = URI.create(action);
        String path = uri.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/cas/login";
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isBlank()) {
            path += "?" + uri.getRawQuery();
        }
        return path;
    }

    private static String ensureServiceParam(String uri) {
        URI u = URI.create(uri);
        String path = u.getRawPath();
        if (path == null || path.isBlank()) {
            path = "/cas/login";
        }
        String q = u.getRawQuery();
        String service = "service=" + URLEncoder.encode(MENJIN_SERVICE, StandardCharsets.UTF_8);
        if (q == null || q.isBlank()) {
            return path + "?" + service;
        }
        if (q.contains("service=")) {
            return path + "?" + q;
        }
        return path + "?" + q + "&" + service;
    }

    private static String encryptRsa(String input) throws Exception {
        ScriptEngine js = new NashornScriptEngineFactory().getScriptEngine();
        js.eval(JavaScriptDes.javascriptCode);
        return (String) js.eval("strEnc('" + input + "','1','2','3')");
    }

    private static String urlEncode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String ua() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0";
    }

    private static String md5Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(b & 0xFF);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static String randomAlphaNum(int len) {
        String chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private static final class ResponseData {
        private final HttpResponseStatus status;
        private final String location;
        private final List<String> setCookies;
        private final String body;
        private final String bodySnippet;

        private ResponseData(HttpResponseStatus status, String location, List<String> setCookies, String body) {
            this.status = status;
            this.location = location;
            this.setCookies = setCookies;
            this.body = body;
            this.bodySnippet = body.substring(0, Math.min(body.length(), 300));
        }
    }
}

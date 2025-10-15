package cn.itcast.server.session;

import cn.itcast.message.GroupCreateResponseMessage;
import cn.itcast.message.GroupJoinRequestMessage;
import cn.itcast.message.GroupJoinResponseMessage;
import cn.itcast.server.service.UserService;
import cn.itcast.server.service.UserServiceFactory;
import cn.itcast.server.service.UserServiceMemoryImpl;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class GroupSessionMemoryImpl implements GroupSession {
    private final Map<String, Group> groupMap = new ConcurrentHashMap<>();
    @Override
    public Group createGroup(String name, Set<String> members, String groupHost) {
        Group group = new Group(name, members, groupHost);
        return groupMap.putIfAbsent(name, group);
    }

    @Override
    public Group joinMember(String name, String member) throws InterruptedException {
        Group group = groupMap.get(name);
        String groupHost = group.getGroupHost();
        //获取通道
        Channel channel = SessionFactory.getSession().getChannel(groupHost);
        if (channel == null) {
            //给请求加群的人返回错误信息
            SessionFactory.getSession().getChannel(member).writeAndFlush(new GroupCreateResponseMessage(false, "群主不在线"));
        }
        if (channel != null) {
            ChannelFuture future = channel.writeAndFlush(new GroupJoinRequestMessage(member, name));
            final GroupJoinResponseMessage[] message = new GroupJoinResponseMessage[1];
            //接收通道收到的消息
            future.addListener(future1 -> {
                if(future1.isSuccess()){
                    message[0] = (GroupJoinResponseMessage) future1.get();
                    log.info("群主审核完毕，下面是群主想法");
                }
            });

                synchronized (member.intern()){
                    log.info("等待群主审核");
                    try {
                        member.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info("群主审核完毕");
                }
                Thread.sleep(100);
                if(message[0] != null && message[0].isSuccess()){
                    log.info("群主审核通过");
                    groupMap.computeIfPresent(name, (key, value) -> {
                        value.getMembers().add(member);
                        return value;
                    });
                }else{
                    log.info("群主审核未通过");
                }
        }
        return null;
    }

    @Override
    public Group removeMember(String name, String member) {
        return groupMap.computeIfPresent(name, (key, value) -> {
            value.getMembers().remove(member);
            return value;
        });
    }

    @Override
    public Group removeGroup(String name) {
        return groupMap.remove(name);
    }

    @Override
    public Set<String> getMembers(String name) {
        return groupMap.getOrDefault(name, Group.EMPTY_GROUP).getMembers();
    }

    @Override
    public List<Channel> getMembersChannel(String name) {
        return getMembers(name).stream()
                .map(member -> SessionFactory.getSession().getChannel(member))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

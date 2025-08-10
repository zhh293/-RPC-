package cn.itcast.message;

import lombok.Data;
import lombok.ToString;

import java.util.Set;

@Data
@ToString(callSuper = true)
public class GroupCreateRequestMessage extends Message {
    private String groupName;
    private Set<String> members;
    private String groupHost;

    public GroupCreateRequestMessage(String groupName, Set<String> members, String groupHost) {
        this.groupName = groupName;
        this.members = members;
        this.groupHost = groupHost;
    }

    @Override
    public int getMessageType() {
        return GroupCreateRequestMessage;
    }
}

package com.example.traveling;
public class ModelGroup {
    private String groupId;
    private String name;
    private String description;
    private String createdBy;
    private String timestamp;
    private int memberCount;
    private int postCount;

    public ModelGroup() {}

    public ModelGroup(String name, String description, String createdBy, String timestamp) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
        this.timestamp = timestamp;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
    public int getPostCount() { return postCount; }
    public void setPostCount(int postCount) { this.postCount = postCount; }
}
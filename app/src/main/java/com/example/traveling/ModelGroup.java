package com.example.traveling;

public class ModelGroup {
    private String groupId;
    private String name;
    private String description;
    private String createdBy;
    private String timestamp;

    public ModelGroup() {}

    public ModelGroup(String name, String description, String createdBy, String timestamp) {
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
package com.meitu.MessageToastDemo;

/**
 * Liu Pei  2018/4/9.
 * Mail ：Lp960822@outlook.com
 */

public class MessageBean {
    private int opType;
    private User user;
    private Gift gift;

    public MessageBean(int opType, User user, Gift gift) {
        this.opType = opType;
        this.user = user;
        this.gift = gift;
    }

    public int getOpType() {
        return opType;
    }

    public void setOpType(int opType) {
        this.opType = opType;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Gift getGift() {
        return gift;
    }

    public void setGift(Gift gift) {
        this.gift = gift;
    }

    static class User{
        private String id;
        private String name;
        private int level;

        public User(String id, String name, int level) {
            this.id = id;
            this.name = name;
            this.level = level;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }
    }

    static class Gift{
        private String id;
        private String name;

        public Gift(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

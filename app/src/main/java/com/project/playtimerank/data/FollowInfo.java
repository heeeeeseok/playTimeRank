package com.project.playtimerank.data;

public class FollowInfo implements Comparable<FollowInfo>{

    private final String userName;
    private final String puuid;
    private final int playTimeMinute;

    public FollowInfo(String userName, String puuid, int playTimeMinute) {
        this.userName = userName;
        this.puuid = puuid;
        this.playTimeMinute = playTimeMinute;
    }

    public String getUserName() {
        return userName;
    }

    public String getPuuid() {
        return puuid;
    }

    public int getPlayTimeMinute() {
        return playTimeMinute;
    }

    @Override
    public int compareTo(FollowInfo followInfo) {
        return followInfo.getPlayTimeMinute() - playTimeMinute;
    }
}

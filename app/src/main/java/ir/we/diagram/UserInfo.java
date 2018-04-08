package ir.we.diagram;

public class UserInfo {
    // TODO a more meaningful way to store

    static int userId;

    public static int getUserId() {
        return userId;
    }

    public static void setUserId(int userId) {
        UserInfo.userId = userId;
    }
}
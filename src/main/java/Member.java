public class Member {
    private Long userId;
    private String name;

    public Member(Long userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public Long getUserId() { return userId; }
    public String getName() { return name; }
}

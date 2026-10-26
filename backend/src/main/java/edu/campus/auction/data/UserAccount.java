package edu.campus.auction.data;

import jakarta.persistence.*;

/** 数据库内部对象。接口通过 DTO 返回数据，避免泄露密码摘要。 */
@Entity
@Table(name = "user_accounts", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 32)
    public String username;
    @Column(nullable = false, length = 100)
    public String passwordHash;
}

package com.project.comgle.entity;


import com.project.comgle.security.UserDetailsImpl;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String memberName;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private String logModify;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Builder
    private Log(String memberName, String content, String logModify, Post post) {
        this.memberName = memberName;
        this.content = content;
        this.logModify = logModify;
        this.post = post;
    }

    public static Log of (String memberName, String content, String logModify, Post post) {
        return Log.builder()
                .memberName(memberName)
                .content(content)
                .logModify(logModify)
                .post(post)
                .build();
    }
}
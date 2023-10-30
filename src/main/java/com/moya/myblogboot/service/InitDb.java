package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.domain.member.Role;
import com.moya.myblogboot.repository.*;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@RequiredArgsConstructor
public class InitDb {
    private final InitService initService;
    private final BoardRepository boardRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final RoleRepository roleRepository;

    @PostConstruct
    public void init() {
        String roleAdmin = initService.initRole("ADMIN");
        String roleNormal = initService.initRole("NORMAL");
        Role role = roleRepository.findOne(roleAdmin).get();
        String username = initService.initAdminMember("moyada123", "moyada123", "Moya", role);

        Long categoryId1 = initService.initCategory("Java");
        Long categoryId2 = initService.initCategory("Python");
        Member adminMember = memberRepository.findOne(username).get();
        Category category1 = categoryRepository.findOne(categoryId1).get();
        Category category2 = categoryRepository.findOne(categoryId2).get();

      // Board 카테고리별로 10개씩
        for (int i = 0; i < 10; i++) {
            Long boardId1 = initService.initBoard(adminMember, category1, "자바", "자바의 진짜 직이네~");
            Long boardId2 = initService.initBoard(adminMember, category2, "파이썬", "파이썬 진짜 멋지네~~");
            Board board1 = boardRepository.findOne(boardId1).get();
            Board board2 = boardRepository.findOne(boardId2).get();
        }
    }
    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {
        private final EntityManager em;
        private final PasswordEncoder passwordEncoder;
        public String initRole (String roleName){
            Role role = Role.builder()
                    .roleName(roleName)
                    .build();
            em.persist(role);
            return role.getRoleName();
        }

        public String initAdminMember(String username, String password, String nickname, Role role){
            Member member = Member.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .nickname(nickname)
                    .build();
            member.addRole(role);
            em.persist(member);
            return member.getUsername();
        }


        public Long initCategory(String categoryName) {
            Category category = Category.builder()
                    .name(categoryName)
                    .build();
            em.persist(category);
            return category.getId();
        }
        public Long initBoard(Member member, Category category, String title, String content) {
            Board board = Board.builder()
                    .member(member)
                    .category(category)
                    .title(title)
                    .content(content)
                    .build();
            em.persist(board);
            return board.getId();
        }
    }
}

package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.board.BoardReqDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
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
    private final CategoryRepository categoryRepository;

    @PostConstruct
    public void init() {
        Member adminMember = initService.initAdminMember();
        Long categoryId1 = initService.initCategory("Java");
        Long categoryId2 = initService.initCategory("Python");
        Category category1 = categoryRepository.findOne(categoryId1).get();
        Category category2 = categoryRepository.findOne(categoryId2).get();

      // Board 카테고리별로 10개씩
        for (int i = 0; i < 10; i++) {
            Long boardId1 = initService.initBoard(adminMember, category1, "자바", "자바의 진짜 직이네~");
            Long boardId2 = initService.initBoard(adminMember, category2, "파이썬", "파이썬 진짜 멋지네~~");
            initService.increment(boardId1);
            initService.increment(boardId2);
        }
    }
    @Component
    @Transactional
    @RequiredArgsConstructor
    static class InitService {
        private final EntityManager em;
        private final PasswordEncoder passwordEncoder;
        private final BoardService boardService;

        public Member initAdminMember(){
            Member member = Member.builder()
                    .username("moyada123")
                    .password(passwordEncoder.encode("moyada123"))
                    .nickname("Moya")
                    .build();

            member.addRoleAdmin();
            em.persist(member);
            return member;
        }

        public Long initCategory(String categoryName) {
            Category category = Category.builder()
                    .name(categoryName)
                    .build();
            em.persist(category);
            return category.getId();
        }

        public Long initBoard(Member member, Category category, String title, String content) {
            BoardReqDto boardReqDto = BoardReqDto.builder().title(title).content(content).category(category.getId()).build();
            return boardService.uploadBoard(boardReqDto, member.getId());
        }

        public void increment(Long boardId){
            boardService.incrementBoardLikeCount(boardId);
        }
    }
}

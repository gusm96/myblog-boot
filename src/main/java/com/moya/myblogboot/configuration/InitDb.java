package com.moya.myblogboot.configuration;
import com.moya.myblogboot.domain.board.BoardReqDto;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.domain.member.Member;
import com.moya.myblogboot.repository.*;
import com.moya.myblogboot.service.BoardService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
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
        Category category1 = categoryRepository.findById(categoryId1).get();
        Category category2 = categoryRepository.findById(categoryId2).get();
        initService.initTestMember();
        // Board 카테고리별로 10개씩
        for (int i = 0; i < 10; i++) {
            initService.initBoard(adminMember, category1, "자바", "자바의 진짜 직이네~");
            initService.initBoard(adminMember, category2, "파이썬", "파이썬 진짜 멋지네~~");
        }
    }
    @Component
    @RequiredArgsConstructor
    static class InitService {
        private final EntityManager em;
        private final PasswordEncoder passwordEncoder;
        private final BoardService boardService;
        private final BoardRepository boardRepository;
        @Transactional
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
        @Transactional
        public void initTestMember(){
            em.setFlushMode(FlushModeType.COMMIT); // Flush 모드를 COMMIT으로 설정
            String password = passwordEncoder.encode("a12345678");
            for (int i = 1; i <= 10000; i++) {
                Member member = Member.builder()
                        .username("liketest" + i)
                        .password(password)
                        .nickname("테스터" + i)
                        .build();
                em.persist(member);
                if (i % 20 == 0) { // 일정 갯수마다 Flush 및 Clear 실행
                    em.flush();
                    em.clear();
                }
            }
        }
        @Transactional
        public Long initCategory(String categoryName) {
            Category category = Category.builder()
                    .name(categoryName)
                    .build();
            em.persist(category);
            return category.getId();
        }
        @Transactional
        public void initBoard(Member member, Category category, String title, String content) {
            BoardReqDto boardReqDto = BoardReqDto.builder()
                    .title(title)
                    .content(content)
                    .category(category.getId())
                    .build();
            boardService.uploadBoard(boardReqDto, member.getId());
        }

    }
}
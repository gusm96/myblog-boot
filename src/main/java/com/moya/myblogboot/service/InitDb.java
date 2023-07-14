package com.moya.myblogboot.service;

import com.moya.myblogboot.domain.admin.Admin;
import com.moya.myblogboot.domain.board.Board;
import com.moya.myblogboot.domain.category.Category;
import com.moya.myblogboot.repository.AdminRepository;
import com.moya.myblogboot.repository.BoardRepository;
import com.moya.myblogboot.repository.CategoryRepository;
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
    private final AdminRepository adminRepository;

    @PostConstruct
    public void init() {
        String adminName = initService.InitAdmin("moya", "moya1343", "Moyada");
        Long categoryId1 = initService.InitCategory("Java");
        Long categoryId2 = initService.InitCategory("Python");
        Admin admin = adminRepository.findById(adminName).get();
        Category category1 = categoryRepository.findOne(categoryId1).get();
        Category category2 = categoryRepository.findOne(categoryId2).get();

      // Board 카테고리별로 10개씩
        for (int i = 0; i < 10; i++) {
            Long boardId1 = initService.InitBoard(admin, category1, "자바", "자바의 진짜 직이네~");
            Long boardId2 = initService.InitBoard(admin, category2, "파이썬", "파이썬 진짜 멋지네~~");
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
        public String InitAdmin(String admin_name, String password, String nickname) {
            Admin admin = Admin.builder()
                    .admin_name(admin_name)
                    .admin_pw(passwordEncoder.encode(password))
                    .nickname(nickname)
                    .build();
            em.persist(admin);
            return admin.getAdmin_name();
        }

        public Long InitCategory(String categoryName) {
            Category category = Category.builder()
                    .name(categoryName)
                    .build();
            em.persist(category);
            return category.getId();
        }

        public Long InitBoard(Admin admin, Category category, String title, String content) {
            Board board = Board.builder()
                    .admin(admin)
                    .category(category)
                    .title(title)
                    .content(content)
                    .build();
            em.persist(board);
            return board.getId();
        }
    }
}

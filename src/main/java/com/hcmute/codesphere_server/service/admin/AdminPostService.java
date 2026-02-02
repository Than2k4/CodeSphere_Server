package com.hcmute.codesphere_server.service.admin;

import com.hcmute.codesphere_server.model.entity.PostEntity;
import com.hcmute.codesphere_server.repository.common.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public Page<PostEntity> getPosts(Pageable pageable, String search, String isBlocked) {
        Specification<PostEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Eager fetch author to avoid lazy loading issues
            root.fetch("author", JoinType.LEFT);
            // Prevent duplicate results
            query.distinct(true);

            // Search by title or content
            if (search != null && !search.trim().isEmpty()) {
                String lowerCaseSearch = search.toLowerCase();
                String searchPattern = "%" + lowerCaseSearch + "%";
                // Search in title (case-insensitive)
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), searchPattern);
                // Search in content - MySQL LOB columns can be searched directly with LIKE
                // We'll search the content as-is (MySQL default collation is case-insensitive)
                Predicate contentPredicate = cb.like(root.get("content"), searchPattern);
                predicates.add(cb.or(titlePredicate, contentPredicate));
            }

            // Filter by blocked status
            if (isBlocked != null && !isBlocked.trim().isEmpty()) {
                boolean blocked = Boolean.parseBoolean(isBlocked);
                predicates.add(cb.equal(root.get("isBlocked"), blocked));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return postRepository.findAll(spec, pageable);
    }

    @Transactional
    public void deletePost(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với ID: " + id));
        postRepository.delete(post);
    }

    @Transactional
    public void toggleBlock(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy post với ID: " + id));
        post.setBlocked(!post.getBlocked());
        postRepository.save(post);
    }
}


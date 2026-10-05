package com.smartshop.category.service;

import com.smartshop.category.dto.CategoryRequest;
import com.smartshop.category.dto.CategoryResponse;
import com.smartshop.category.entity.Category;
import com.smartshop.category.mapper.CategoryMapper;
import com.smartshop.category.repository.CategoryRepository;
import com.smartshop.common.exception.BusinessException;
import com.smartshop.common.exception.ResourceNotFoundException;
import com.smartshop.common.mapping.TextNormalizer;
import com.smartshop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getEntity(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        if (categoryRepository.existsByNameIgnoreCase(category.getName())) {
            throw duplicateName(category.getName());
        }
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getEntity(id);
        // Verifier AVANT de modifier l'entite managee : sinon l'auto-flush d'Hibernate avant la requete
        // ecrirait la modification et la contrainte SQL repondrait a la place de ce controle.
        String name = TextNormalizer.trimToNull(request.name());
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw duplicateName(name);
        }
        categoryMapper.update(request, category);
        return categoryMapper.toResponse(category);
    }

    /** Suppression reelle, refusee si des produits utilisent encore la categorie. */
    @Transactional
    public void delete(Long id) {
        Category category = getEntity(id);
        long products = productRepository.countByCategoryId(id);
        if (products > 0) {
            throw new BusinessException(
                    "Catégorie utilisée par %d produit(s) : impossible de la supprimer".formatted(products));
        }
        categoryRepository.delete(category);
    }

    /** Utilise par les autres services pour rattacher une categorie existante. */
    @Transactional(readOnly = true)
    public Category getEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", id));
    }

    private static BusinessException duplicateName(String name) {
        return new BusinessException("Une catégorie nommée '%s' existe déjà".formatted(name));
    }
}

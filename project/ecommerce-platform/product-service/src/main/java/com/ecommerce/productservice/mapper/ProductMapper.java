package com.ecommerce.productservice.mapper;

import com.ecommerce.productservice.dto.request.CreateProductRequest;
import com.ecommerce.productservice.dto.request.UpdateProductRequest;
import com.ecommerce.productservice.dto.response.ProductResponse;
import com.ecommerce.productservice.entity.ProductEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount",   ignore = true)
    @Mapping(target = "active",        ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "createdBy",     ignore = true)
    @Mapping(target = "updatedBy",     ignore = true)
    @Mapping(target = "version",       ignore = true)
    ProductEntity toEntity(CreateProductRequest request);

    /** inStock = stockQuantity > 0 */
    @Mapping(target = "inStock", expression = "java(entity.getStockQuantity() > 0)")
    ProductResponse toResponse(ProductEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",            ignore = true)
    @Mapping(target = "sku",           ignore = true)
    @Mapping(target = "averageRating", ignore = true)
    @Mapping(target = "reviewCount",   ignore = true)
    @Mapping(target = "createdAt",     ignore = true)
    @Mapping(target = "updatedAt",     ignore = true)
    @Mapping(target = "createdBy",     ignore = true)
    @Mapping(target = "updatedBy",     ignore = true)
    @Mapping(target = "version",       ignore = true)
    void updateEntityFromRequest(UpdateProductRequest request, @MappingTarget ProductEntity entity);
}

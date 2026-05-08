package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.response.OrderItemResponse;
import com.ecommerce.orderservice.dto.response.OrderResponse;
import com.ecommerce.orderservice.entity.OrderEntity;
import com.ecommerce.orderservice.entity.OrderItemEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(OrderEntity entity);

    @Mapping(target = "lineTotal",
             expression = "java(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))")
    OrderItemResponse toItemResponse(OrderItemEntity item);
}

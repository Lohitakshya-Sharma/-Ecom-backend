package com.ecommerce.mapper;

import com.ecommerce.dto.OrderItemResponseDTO;
import com.ecommerce.dto.OrderResponseDTO;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponseDTO toOrderResponseDTO(Order order);

    @Mapping(target = "productId", source = "product.id")
    OrderItemResponseDTO toOrderItemResponseDTO(OrderItem item);
}

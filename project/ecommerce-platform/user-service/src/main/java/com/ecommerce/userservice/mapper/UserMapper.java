package com.ecommerce.userservice.mapper;

import com.ecommerce.userservice.dto.request.RegisterRequest;
import com.ecommerce.userservice.dto.response.UserResponse;
import com.ecommerce.userservice.entity.UserEntity;
import org.mapstruct.*;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserMapper {

    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "password",              ignore = true)  // hashed separately
    @Mapping(target = "role",                  ignore = true)  // defaulted to USER
    @Mapping(target = "enabled",               ignore = true)
    @Mapping(target = "failedLoginAttempts",   ignore = true)
    @Mapping(target = "createdAt",             ignore = true)
    @Mapping(target = "updatedAt",             ignore = true)
    @Mapping(target = "createdBy",             ignore = true)
    @Mapping(target = "updatedBy",             ignore = true)
    @Mapping(target = "version",               ignore = true)
    UserEntity toEntity(RegisterRequest request);

    /**
     * Converts UserEntity to UserResponse DTO for API responses.
     *
     * All audit fields from BaseEntity are included in UserResponse.
     * password is NOT in UserResponse (different class, no mapping needed).
     */
    UserResponse toResponse(UserEntity entity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id",                    ignore = true)
    @Mapping(target = "email",                 ignore = true)
    @Mapping(target = "username",              ignore = true)
    @Mapping(target = "password",              ignore = true)
    @Mapping(target = "role",                  ignore = true)
    @Mapping(target = "enabled",               ignore = true)
    @Mapping(target = "failedLoginAttempts",   ignore = true)
    @Mapping(target = "createdAt",             ignore = true)
    @Mapping(target = "updatedAt",             ignore = true)
    @Mapping(target = "createdBy",             ignore = true)
    @Mapping(target = "updatedBy",             ignore = true)
    @Mapping(target = "version",               ignore = true)
    void updateEntityFromRequest(
            com.ecommerce.userservice.dto.request.UpdateProfileRequest request,
            @MappingTarget UserEntity entity
    );
}

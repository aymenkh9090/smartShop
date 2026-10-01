package com.smartshop.user.mapper;

import com.smartshop.user.dto.UserResponse;
import com.smartshop.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}

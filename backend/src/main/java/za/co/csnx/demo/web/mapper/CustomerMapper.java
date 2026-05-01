package za.co.csnx.demo.web.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import za.co.csnx.demo.domain.Customer;
import za.co.csnx.demo.web.dto.CustomerDto;

@Mapper
public interface CustomerMapper {

    @Mapping(target = "memberSince", source = "createdAt")
    CustomerDto toDto(Customer customer);
}

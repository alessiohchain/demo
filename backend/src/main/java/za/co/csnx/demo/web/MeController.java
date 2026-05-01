package za.co.csnx.demo.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import za.co.csnx.demo.service.CustomerService;
import za.co.csnx.demo.web.dto.CustomerDto;
import za.co.csnx.demo.web.mapper.CustomerMapper;

@RestController
@RequestMapping("/api/me")
public class MeController {

    private final CustomerService customerService;
    private final CustomerMapper customerMapper;

    public MeController(CustomerService customerService, CustomerMapper customerMapper) {
        this.customerService = customerService;
        this.customerMapper = customerMapper;
    }

    @GetMapping
    public CustomerDto me(@AuthenticationPrincipal String email) {
        return customerMapper.toDto(customerService.findByEmail(email));
    }
}

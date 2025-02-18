package com.mlbeez.feeder.service;
import com.mlbeez.feeder.model.AspNetRole;
import com.mlbeez.feeder.model.AspNetUserRole;
import com.mlbeez.feeder.model.UserResponseBaseModel;
import com.mlbeez.feeder.repository.AspNetRoleRepository;
import com.mlbeez.feeder.repository.AspNetUserRepository;
import com.mlbeez.feeder.repository.AspNetUserRoleRepository;
import com.mlbeez.feeder.service.exception.DataNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class JwtUserDetailsService implements UserDetailsService {

    @Autowired
    private AspNetUserRepository aspNetUserRepository;

    @Autowired
    private AspNetUserRoleRepository aspNetUserRoleRepository;

    @Autowired
    private AspNetRoleRepository aspNetRoleRepository;


    @Override
    public UserDetails loadUserByUsername(String userName) {

        UserResponseBaseModel userResponseBaseModel = aspNetUserRepository.findByUsername(userName);
        if (userResponseBaseModel == null) {
            throw new DataNotFoundException("User not found: " + userName);
        }

        AspNetUserRole userRole = aspNetUserRoleRepository.findByUserId(userResponseBaseModel.getId())
                .orElseThrow(() -> new DataNotFoundException("Role not found for user: " + userName));

        AspNetRole role = aspNetRoleRepository.findById(userRole.getRoleId())
                .orElseThrow(() -> new DataNotFoundException("Role details not found for user: " + userName));

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));

        return new User(userResponseBaseModel.getUsername(), userResponseBaseModel.getPasswordHash(), authorities);
    }

}

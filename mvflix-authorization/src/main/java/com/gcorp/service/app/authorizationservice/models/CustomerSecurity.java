package com.gcorp.service.app.authorizationservice.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
public class CustomerSecurity implements UserDetails
{
    private String username;
    private String password;
    private Role role;

    public CustomerSecurity(String username, String password, Role role)
    {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        List<SimpleGrantedAuthority> stream = this.role.getAuthorizations()
                .stream()
                .map(authority -> new SimpleGrantedAuthority(authority.authName))
                .toList();

        List<SimpleGrantedAuthority> authorities = new ArrayList<>(stream);
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.getRoleName()));

        return authorities;
    }
}

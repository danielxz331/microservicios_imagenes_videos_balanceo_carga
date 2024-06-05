
package com.example.usuarios.service;

import com.example.usuarios.DTO.UserDTO;
import com.example.usuarios.model.User;
import com.example.usuarios.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;
    
    public User createUser (User user){
        return userRepository.save(user);
    }
    
    public boolean existsByUsername(String username){
        boolean userExists = userRepository.findByUsername(username).isPresent();
        return userExists;
    }
    
    public List<UserDTO> listUsers(){
        return userRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    public Optional<UserDTO> getUserById(int id){
        return userRepository.findById(id).map(this::convertToDTO);
    }
    
    public Optional<UserDTO> getUserByUsername(String username){
        return userRepository.findByUsername(username).map(this::convertToDTO);
    }
    
    private UserDTO convertToDTO(User user){
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setName(user.getName());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        return userDTO;
    }
}

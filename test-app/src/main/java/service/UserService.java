package service;

import model.User;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final List<User> users = new ArrayList<>();

    public User findById(Long id) {
        for (User user : users) {
            if (user.getId().equals(id)) {
                return user;
            }
        }
        return null;
    }

    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    public User save(User user) {
        users.add(user);
        return user;
    }

    public int countActiveUsers() {
        int count = 0;
        for (User user : users) {
            if (user.isActive()) {
                count++;
            }
        }
        return count;
    }
}

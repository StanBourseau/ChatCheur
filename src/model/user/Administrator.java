package model.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Administrator extends Moderator{

    public Administrator(int id, String userName, String password, String email, String firstName, String lastName, State state, LocalDateTime lastConnectionTime) {
        super(id, userName, password, email, firstName, lastName, state, lastConnectionTime);
        setPermission(Permission.ADMINISTRATOR);
        //java.DAO update:
        //userDAO.update(this);
    }

    public void changeStatus(User user, Permission permission){

        switch (permission) {
            case ADMINISTRATOR -> {
                Administrator administrator = new Administrator(user.getId(), user.getUserName(), user.getPassword(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getState(), user.getLastConnectionTime());
                //Replace the old object by the new one:
                //userDAO.update(administrator);
            }
            case MODERATOR -> {
                Moderator moderator = new Moderator(user.getId(), user.getUserName(), user.getPassword(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getState(), user.getLastConnectionTime());
                //Replace the old object by the new one:
                //userDAO.update(moderator);
            }
            case USER -> {
                User user1 = new User(user.getId(), user.getUserName(), user.getPassword(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getState(), user.getLastConnectionTime());
                //Replace the old object by the new one:
                //userDAO.update(user1);
            }
        }

    }


}

package controller;

import DAO.LogDao;
import DAO.MessageDao;
import DAO.UserDao;
import model.Log;
import model.Message;
import model.user.User;
import view.*;
import server.ChatcheurThread;
import view.Menu;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.List;
import java.util.Objects;

public class ClientController implements ActionListener {
    private final LogDao logDao = new LogDao();
    private final MessageDao messageDao = new MessageDao();
    private final UserDao userDao = new UserDao();
    private Menu view1;
    private Home view2;
    private LogOut view3;
    private NewPassword view4;
    private Stats view5;
    private Settings view6;
    private InfoUser view7;
    private User currentUser;
    private List<User> users;
    private List<Log> logs;
    private List<Message> messages;
    private PrintWriter out;
    private Color C1, C2, C3, C4, C5, C6;

    public ClientController(List<User> users, List<Log> logs, List<Message> messages, Menu view, Socket socket) {
        this.currentUser = null;
        this.users = users;
        this.logs = logs;
        this.messages = messages;
        this.view1 = view;
        this.view2 = null;
        this.view3 = null;
        this.view4 = null;
        this.view5 = null;
        this.view6 = null;
        this.view7 = null;
        view1.addAllListener(this);
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashInBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashInBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public List<User> getUsers() {
        return users;
    }

    public List<Log> getLogs() {
        return logs;
    }

    public void setLogs(List<Log> logs) {
        this.logs = logs;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }



    //--------------------------CONNECTION-------------------------------
    public void connection(String username, String psw) {
        boolean userFinded = false;
        //On parcourt tous les users
        for (User user : this.users) {
            //On cherche un user avec le nom et le mdp correspondent
            //chiffrer le mdp en SHA-256

            if (user.getUserName().equals(username)) {
                if (user.getUserName().equals(username) && user.getPassword().equals(sha256(psw))) {
                    userFinded = true;
                    System.out.println("User trouve : " + username);
                    //On regarde si le user est banni
                    if (user.getAccess().equals(User.Access.ACCEPTED)) {
                        this.currentUser = user;
                        this.currentUser.setState(User.State.ONLINE);
                        this.currentUser.setLastConnectionTime(LocalDateTime.now());

                        gererFenetresConnection();
                        sendToServerConnection();
                        connectionToDB(this.currentUser);

                    } else {
                        System.out.println("Connexion refusee, le user est banni");
                        view1.afficherBannissement();
                    }
                } else if (user.getUserName().equals(username)) {
                    userFinded = true;
                    System.out.println("Mdp incorrect");
                    view1.afficherMdpIncorrect();
                }
            }
        }
        if (this.currentUser == null && !userFinded) {
            System.out.println("Aucun utilisateur trouve");
            view1.afficherUserUknown();
        }
        //view2.repaint();
    }

    public void connectionToDB(User user) {
        //On met a jour BDD
        this.userDao.update(user);
        //Création d'un log connection
        Log logConnection = new Log(user.getId(), Log.TypeLog.CONNECTION);
        //On ajoute le log dans la BDD
        logDao.create(logConnection);
    }

    public void gererFenetresConnection() {
        //On crée fenetre
        try {
            //On supprime menu
            this.view1.dispose();
            this.view2 = new Home(users, messages, view1.getUsername(), C1, C2, C3, C4, C5, C6);
            //On met la 1ere fenetre a null
            this.view1 = null;
            this.view2.addAllListener(this);
            this.view2.setVisible(true);
            this.view2.setResizable(true);
            this.view2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.view2.setLocationRelativeTo(null);
            view2.repaint();
        } catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    //-----------------------------MESSAGE---------------------------------

    public void send(String message) {
        if (view2.getTextField().getText().contains("'")) {
            message = message.replace("'", "‘");
        }
        if (!message.equals("Saisir du texte") && !message.isEmpty() && currentUser != null) {
            Message messagToSend = new Message(currentUser.getId(), message, messageDao.getLastID() + 1);
            Log logToSend = new Log(currentUser.getId(), Log.TypeLog.MESSAGE);
            sendToServerMessage(messagToSend);
            //On met a jour la vue
            view2.setInputReceived(true);
            messages.add(messagToSend);
            int y = view2.calculY(messages);
            if (message.charAt(0) == '/') {
                ImageIcon imageIcon = new ImageIcon("imageEnvoyees" + message);
                int imageIconWidth = imageIcon.getIconWidth();
                int imageIconHeight = imageIcon.getIconHeight();
                double ratio = (double) imageIconWidth / (double) imageIconHeight;
                if (imageIconWidth > 300) {
                    imageIconWidth = 500;
                    imageIconHeight = (int) (imageIconWidth / ratio);
                }
                if (imageIconHeight > 300) {
                    imageIconHeight = 300;
                }
                y -= imageIconHeight + 70;
            }
            messages.remove(messagToSend);
            view2.getScrollPane().getVerticalScrollBar().setValue(view2.getScrollPane().getVerticalScrollBar().getMaximum());
            view2.getconversationPanelContent().setPreferredSize(new Dimension(950, y + 60));
            view2.getScrollPane().getViewport().setViewPosition(new Point(0, y));
            view2.setY(y);
            view2.getTextField().setText(null);
            view2.repaint();

            try {
                messageDao.create(messagToSend);
                logDao.create(logToSend);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //-------------------------DISCONNECTION--------------------------------------
    public void disconnection(boolean sendToServer) {
        this.currentUser.setState(User.State.OFFLINE);
        if (sendToServer) sendToServerDisconnection();
        Log logDeconnection = new Log(currentUser.getId(), Log.TypeLog.DISCONNECTION);
        //On met a jour BDD
        logDao.create(logDeconnection);
        this.userDao.update(currentUser);
        this.currentUser = null;
        gererFenetresDisconnection();
    }

    public void gererFenetresDisconnection() {
        //On ferme les autres fenetres
        if (view3 != null) {
            view3.dispose();
            view3 = null;
        }
        view2.dispose();
        view2 = null;
        //On crée la fenetre de base
        try {
            view1 = new Menu(users, logs, messages);
            view1.addAllListener(this);
        } catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void gererFenetresLogOut() {
        view3 = new LogOut(view2, C1, C2, C3, C4, C5, C6);
        view3.setVisible(true);
        view3.addAllListener(this);
    }

    public void infoUser(int i) {
        try {
            view7 = new InfoUser(users.get(i - 1), currentUser);
            view7.addAllListener(this);
        } catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view7.setVisible(true);
    }

    //----------------------------------BANNISSEMENT-----------------------------------------

    public void bannissement(int i) {
        //On cree nouvelle list sans le current user
        List<User> nonCurrentUsers = new ArrayList<>();
        User userToChange = null;
        int positionIcon = 0;
        for (User user : this.users) {
            if (user.getId() != (this.currentUser.getId())) {
                nonCurrentUsers.add(user);
                if (user.getId() == i) {
                    userToChange = user;
                }
            }

        }

        //Si l'utilisateur est banni, on le débanni, sinon on le banni
        if (userToChange.getAccess().equals(User.Access.BANNED)) {
            int response = JOptionPane.showConfirmDialog(null, "Êtes-vous sûr de vouloir débannir cet utilisateur ?", "Confirmer le débannissement", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                //A trouver une solution pour le i
                //view2.setIconBan(positionIcon);
                for (User userJava : users) {
                    if (userJava.getId() == userToChange.getId()) {
                        userJava.setAccess(User.Access.ACCEPTED);
                    }
                }
                view2.repaint();
                userDao.update(userToChange);
                Log logBan = new Log(userToChange.getId(), Log.TypeLog.UNBAN);
                logDao.create(logBan);
            }
        } else {
            int response = JOptionPane.showConfirmDialog(null, "Êtes-vous sûr de vouloir bannir cet utilisateur ?", "Confirmer le bannissement", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                //view2.setIconUnban(positionIcon);
                for (User userJava : users) {
                    if (userJava.getId() == userToChange.getId()) {
                        userJava.setAccess(User.Access.BANNED);
                    }
                }
                view2.repaint();

                userDao.update(userToChange);
                Log logBan = new Log(userToChange.getId(), Log.TypeLog.BAN);
                logDao.create(logBan);
            }
        }
        sendToServerChange(userToChange);
    }

    //-----------------------------------ENVOIE SERVEUR-----------------------------------------
    public void sendToServerConnection() {
        this.out.println("Connection: " + this.currentUser.getUserName() + " connected to server " + this.currentUser);
    }

    public void sendToServerChange(User userChanged) {
        this.out.println("Change: " + userChanged);
    }

    public void sendToServerRole(User userChanged) {
        this.out.println("Role: " + userChanged);
    }

    public void sendToServerMessage(Message message) {
        try {
            this.out.println("Message de " + this.currentUser.getUserName() + " : " + message);
        } catch (Exception e) {
            System.out.println("ERROR, Exception occured on sending");
        }
    }

    public void sendToServerDisconnection() {
        this.out.println("Disconnection: " + currentUser.getUserName() + " disconnected from server " + this.currentUser);
    }

    public void sendToServeurEnd(){
        this.out.println("End");
    }

    //-------------------------------------PASSWORD-------------------------------------------
    public void mdpOublie() {
        try {
            view4 = new NewPassword();
            view4.addAllListener(this);
        } catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view4.setVisible(true);
    }

    public void newMdp() {
        currentUser = userDao.findUserName(view4.getTextFieldUserName());
        currentUser.setPassword(view4.getTextFieldNewPassword());
        userDao.update(currentUser);
        currentUser = null;
        view4.dispose();
        view4 = null;
    }

    //-----------------------------------STATS------------------------------------------------
    public void pageStats() {
        try {
            view5 = new Stats(getTypeUser(), getTypeModerator(), getTypeAdministrator(),
                    getUsersOnline(), getUsersAway(), getUsersOffline(),
                    getNumberBanned(), getNumberMessagesPerHour(), getNumberConnectionsPerHour(), getTopUsers(),
                    C1, C2, C3, C4, C5, C6);
            //view5.addAllListener(this);

        } catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view5.setVisible(true);

    }

    public ArrayList<User> getUsersOnline() {
        return userDao.findNumberUsersOnline();
    }

    public ArrayList<User> getUsersAway() {
        return userDao.findNumberUsersAway();
    }

    public ArrayList<User> getUsersOffline() {
        return userDao.findNumberUsersOffline();
    }

    public ArrayList<User> getTypeUser() {
        return userDao.findNumberUser();
    }

    public ArrayList<User> getTypeModerator() {
        return userDao.findNumberModerator();
    }

    public ArrayList<User> getTypeAdministrator() {
        return userDao.findNumberAdministrator();
    }

    public ArrayList<User> getNumberBanned() {
        return userDao.findNumberBanned();
    }

    public ArrayList<Integer> getNumberMessagesPerHour() {

        ArrayList<Integer> finalList = new ArrayList<>();
        LocalDateTime timeNow = LocalDateTime.now();
        LocalDateTime firstHour = LocalDateTime.of(timeNow.getYear(), timeNow.getMonth(), timeNow.getDayOfMonth() - 2, 0, 0);
        LocalDateTime secondHour = firstHour.plusHours(1);

        for (int i = 0; i < 24; i++) {
            finalList.add(messageDao.retrieveMessagesEachHour(firstHour, secondHour));
            firstHour = firstHour.plusHours(1);
            secondHour = secondHour.plusHours(1);
        }
        return finalList;
    }

    public ArrayList<Integer> getNumberConnectionsPerHour() {
        ArrayList<Integer> finalList = new ArrayList<>();
        LocalDateTime timeNow = LocalDateTime.now();
        LocalDateTime firstHour = LocalDateTime.of(timeNow.getYear(), timeNow.getMonth(), timeNow.getDayOfMonth(), 0, 0);
        LocalDateTime secondHour = firstHour.plusHours(1);

        for (int i = 0; i < 24; i++) {
            finalList.add(logDao.findConnectionsPerHour(firstHour, secondHour));
            firstHour = firstHour.plusHours(1);
            secondHour = secondHour.plusHours(1);
        }
        return finalList;

    }

    public ArrayList<User> getTopUsers() {
        ArrayList<User> topUsers = new ArrayList<>();

        for (Integer i : messageDao.findTopUsers()) {
            topUsers.add(userDao.find(i));
        }
        return topUsers;
    }

    public void contenuIntrouvable() {
        MessageDao messageDao = new MessageDao();
        messageDao.delete(messages.get(messages.size() - 1).getId());
        messages.remove(messages.size() - 1);
        JOptionPane.showMessageDialog(view2, "Image introuvable, veuillez charger votre image sous le bon nom dans le fichier imageEnvoyees", "Erreur de chargement d'image", JOptionPane.ERROR_MESSAGE);
        view2.repaint();
    }

    public void setUser(User user) {
        this.users.set(user.getId() - 1, user);
    }

    public void setMessage(Message message) {
        this.messages.set(message.getId() - 1, message);
    }

    public Home getView2() {
        return view2;
    }

    public void pageSettings() {
        try {
            view6 = new Settings(C1, C2, C3, C4, C5, C6);
            view6.addAllListener(this);
        } catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view6.setVisible(true);
    }

    public void changeUsn() {
        if (!Objects.equals(view6.getTextField1().getText(), "")) {
            currentUser.setUserName(view6.getTextField1().getText());
            userDao.update(currentUser);
            sendToServerChange(currentUser);
            view6.dispose();
            view6 = null;
            view2.repaint();
        }
    }

    public void changePsswrd() {
        if (!Objects.equals(view6.getTextField2().getText(), "")) {
            currentUser.setPassword(sha256(view6.getTextField2().getText()));
            userDao.update(currentUser);
            sendToServerChange(currentUser);
            view6.dispose();
            view6 = null;
        }
    }

    public void initialColors(){
        if (C1 == null && C2 == null && C3 == null && C4 == null && C5 == null && C6 == null) {
            C1 = new Color(238, 213, 173);
            C2 = new Color(147, 185, 175);
            C3 = new Color( 27,47,46);
            C4 = new Color(140,56,6);
            C5 = new Color(183, 90, 25);
            C6 = Color.WHITE;
        }
    }
    public void setTheme1(){
        C1 = new Color(0, 0, 173);
        C2 = new Color(147, 185, 175);
        C3 = new Color( 27,47,46);
        C4 = new Color(140,56,6);
        C5 = Color.WHITE;
        C6 = Color.BLACK;
        view2.dispose();
        try {
            view2 = new Home(users, messages, currentUser.getUserName(), C1, C2, C3, C4, C5, C6);
            view2.addAllListener(this);
            view2.setVisible(true);
            view2.setResizable(true);
            view2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            view2.setLocationRelativeTo(null);
            view2.repaint();
        }catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view6.dispose();
        pageSettings();
    }

    public void setTheme2(){
        C1 = new Color(200, 0, 173);
        C2 = new Color(147, 185, 175);
        C3 = new Color( 27,47,46);
        C4 = new Color(140,56,6);
        C5 = Color.WHITE;
        C6 = Color.BLACK;
        view2.dispose();
        try {
            view2 = new Home(users, messages, currentUser.getUserName(), C1, C2, C3, C4, C5, C6);
            view2.addAllListener(this);
            view2.setVisible(true);
            view2.setResizable(true);
            view2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            view2.setLocationRelativeTo(null);
            view2.repaint();
        }catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view6.dispose();
        pageSettings();
    }

    public void setTheme3(){
        C1 = new Color(0, 0, 173);
        C2 = new Color(147, 185, 175);
        C3 = new Color( 27,47,46);
        C4 = new Color(140,56,6);
        C5 = Color.WHITE;
        C6 = Color.BLACK;
        view2.dispose();
        try {
            view2 = new Home(users, messages, currentUser.getUserName(), C1, C2, C3, C4, C5, C6);
            view2.addAllListener(this);
            view2.setVisible(true);
            view2.setResizable(true);
            view2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            view2.setLocationRelativeTo(null);
            view2.repaint();
        }catch (IOException | FontFormatException ex) {
            throw new RuntimeException(ex);
        }
        view6.dispose();
        pageSettings();
    }

    public void addImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "gif"));
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            File destination = new File("imageEnvoyees/" + file.getName());
            try {
                Files.copy(file.toPath(), destination.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
            send("/" + file.getName());
            view2.getTextField().setForeground(Color.BLACK);
        }
    }

    public void newRole() {
        String selected = (String) view7.getComboBox().getSelectedItem();
        User user = users.get(view7.getSelectedUser().getId() - 1);
        if (selected != null) {
            switch (selected) {
                case "ADMINISTRATOR" -> user.setPermission(User.Permission.ADMINISTRATOR);
                case "MODERATOR" -> user.setPermission(User.Permission.MODERATOR);
                case "USER" -> user.setPermission(User.Permission.USER);
            }
        }
        sendToServerRole(user);
        userDao.update(user);
    }

    public void changeUserStatus() {
        switch (currentUser.getState()) {
            case ONLINE -> {
                currentUser.setState(User.State.AWAY);
                view2.getStatusCurrent().setText("Away");
                view2.setColorStatusCurrent(Color.ORANGE);
            }
            case AWAY -> {
                currentUser.setState(User.State.ONLINE);
                view2.getStatusCurrent().setText("Online");
                view2.setColorStatusCurrent(Color.GREEN);
            }
        }
        userDao.update(currentUser);
        sendToServerChange(currentUser);
        view2.getStatusCurrent().setContentAreaFilled(false);
        view2.getStatusCurrent().setBorder(null);
        view2.getStatusCurrent().setForeground(Color.WHITE);
        view2.getStatusCurrent().setOpaque(false);
        view2.getStatusCurrent().setFocusable(false);
        view2.repaint();
    }

    public void closing() {
        currentUser.setState(User.State.AWAY);
        userDao.update(currentUser);
        view2.dispose();
        sendToServeurEnd();
        // Fermer la fenêtre
    }

    //------------------------------LISTENERS------------------------------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        String[] actionCommand = e.getActionCommand().split(" ");
        switch (actionCommand[0]) {
            //Gère la connexion
            case "Connexion" -> {
                initialColors();
                connection(view1.getUsername(), view1.getPassword());
            }

            //Gère la déconnexion graphiquement
            case "logOut" -> {
                initialColors();
                gererFenetresLogOut();
            }

            //Gère la déconnexion hors graphique
            case "Disconnection" -> disconnection(true);

            //Gère le bannissement
            case "Ban" -> bannissement(Integer.parseInt(actionCommand[1]));

            case "Info" -> infoUser(Integer.parseInt(actionCommand[1]));

            //Gère la modification de son mdp
            case "Ok" -> newMdp();

            //Gère l'envoie de message
            case "send" -> send(view2.getTextField().getText());

            //Gère l'oublie de mdp
            case "mdpOublie" -> mdpOublie();

            //Gère les stats
            case "Stats" -> {
                initialColors();
                pageStats();
            }

            case "SmileyIntrouvable", "ImageIntrouvable" -> contenuIntrouvable();

            case "Settings" -> {
                initialColors();
                pageSettings();
            }

            case "changeUsername" -> changeUsn();

            case "changePassword" -> changePsswrd();

            case "addImage" -> addImage();

            case "newRole" -> newRole();

            case "changeUserStatus" -> changeUserStatus();

            case "closing" -> closing();

            case "theme1" -> setTheme1();

            case "theme2" -> setTheme2();

            case "theme3" -> setTheme3();
        }
    }

    public User getCurrentUser() {
        return currentUser;
    }

    //Listener pour bouton connection
}

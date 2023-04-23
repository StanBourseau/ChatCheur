package view;

import DAO.UserDao;
import controller.ClientController;
import model.user.User;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class InfoUser extends JDialog {
    private JComboBox<String> permissionBox;
    private User user;
    public InfoUser(User user, User currentUser) throws IOException, FontFormatException {
        this.user = user;
        setBounds(500, 150, 600, 600);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("Avenir Next.ttc")).deriveFont(25f);
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.setColor(new Color(20, 48, 46));
                g.fillRoundRect(100, 20, 400, 90, 80, 80);
                g.setColor(Color.WHITE);
                g.setFont(customFont.deriveFont(40f));
                g.drawString("Informations", 175, 75);
                g.fillRoundRect(35, 190, 250, 80, 80, 80);
                g.fillRoundRect(315, 190, 250, 80, 80, 80);
                g.fillRoundRect(35, 360, 250, 80, 80, 80);
                g.fillRoundRect(315, 360, 250, 80, 80, 80);
                g.setFont(customFont.deriveFont(30f));
                g.drawString("Nom", 125, 180);
                g.drawString("Prénom", 380, 180);
                g.drawString("Pseudo", 110, 350);
                g.drawString("Role", 410, 350);

                g.setColor(Color.BLACK);
                g.setFont(customFont.deriveFont(25f));
                FontMetrics metrics = g.getFontMetrics(customFont.deriveFont(25f));
                int x1 = (330 - metrics.stringWidth(user.getLastName())) / 2;
                int x2 = (890 - metrics.stringWidth(user.getFirstName())) / 2;
                int x3 = (330 - metrics.stringWidth(user.getUserName())) / 2;
                int x4 = (890 - metrics.stringWidth(user.getPermission().name())) / 2;
                g.drawString(user.getLastName(), x1, 240);
                g.drawString(user.getFirstName(), x2, 240);
                g.drawString(user.getUserName(), x3, 410);
                if(!currentUser.getPermission().equals(User.Permission.ADMINISTRATOR)){
                    g.drawString(user.getPermission().name(), x4, 410);
                }
            }
        };
        panel.setBackground(new Color(147, 185, 175));
        panel.setLayout(null);

        String[] options = {"ADMINISTRATOR", "MODERATOR", "USER"};
        permissionBox = new JComboBox<>(options);
        permissionBox.setActionCommand("NewRole");
        permissionBox.setBounds(350, 372, 200, 50);
        permissionBox.setFont(customFont.deriveFont(20f));
        permissionBox.setEditable(true);
        permissionBox.setFocusable(false);
        permissionBox.setSelectedItem(user.getPermission().name());

        /*permissionBox.addActionListener(e -> {
            String selected = (String) permissionBox.getSelectedItem();
            if (selected != null) {
                switch (selected) {
                    case "ADMINISTRATOR" -> user.setPermission(User.Permission.ADMINISTRATOR);
                    case "MODERATOR" -> user.setPermission(User.Permission.MODERATOR);
                    case "USER" -> user.setPermission(User.Permission.USER);
                }
            }
            userDao.update(user);
        });*/
        panel.add(permissionBox);

        if(!currentUser.getPermission().equals(User.Permission.ADMINISTRATOR)){
            permissionBox.setVisible(false);
        }
        getContentPane().add(panel);
    }

    public JComboBox<String> getPermissionBox() {
        return permissionBox;
    }

    public User getUser() {
        return user;
    }

    public void addAllListener(ClientController controller) {
        this.permissionBox.addActionListener(controller);
    }
}
